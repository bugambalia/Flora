(function () {
  const DEFAULT_OWNER = "bugambalia";
  const DEFAULT_REPO = "Flora";

  const state = {
    owner: DEFAULT_OWNER,
    repo: DEFAULT_REPO,
    catalog: null,
    releases: [],
    searchQuery: "",
    activeCategory: "all",
  };

  function detectGitHubRepo() {
    const params = new URLSearchParams(window.location.search);
    const paramRepo = params.get("repo");
    if (paramRepo && paramRepo.includes("/")) {
      const [owner, repo] = paramRepo.split("/");
      return { owner: owner.trim(), repo: repo.trim() };
    }
    const host = window.location.hostname;
    if (host.endsWith(".github.io")) {
      const owner = host.replace(".github.io", "");
      const segments = window.location.pathname.split("/").filter(Boolean);
      const repo = segments[0] || DEFAULT_REPO;
      return { owner, repo };
    }
    return { owner: DEFAULT_OWNER, repo: DEFAULT_REPO };
  }

  function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text == null ? "" : String(text);
    return div.innerHTML;
  }

  function formatDate(isoDate) {
    if (!isoDate) return "Sin fecha";
    const parsed = new Date(isoDate);
    if (Number.isNaN(parsed.getTime())) return isoDate;
    return parsed.toLocaleDateString("es-ES", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  }

  function formatBytes(bytes) {
    if (!bytes || bytes <= 0) return "";
    const mb = bytes / (1024 * 1024);
    if (mb >= 1) return `${mb.toFixed(2)} MB`;
    return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }

  function classifyHeading(headingText) {
    const h = headingText.toLowerCase();
    if (h.includes("novedad") || h.includes("added") || h.includes("nuevo")) return { category: "added", label: "Novedades" };
    if (h.includes("mejor") || h.includes("changed") || h.includes("improv")) return { category: "improved", label: "Mejoras" };
    if (h.includes("fix") || h.includes("correc") || h.includes("bug")) return { category: "fixed", label: "Correcciones" };
    return { category: "technical", label: headingText.replace(/^#+\s*/, "") || "Notas técnicas" };
  }

  function parseMarkdownSections(markdown) {
    if (!markdown) return [];
    const lines = markdown.split(/\r?\n/);
    const sections = [];
    let current = null;

    for (const rawLine of lines) {
      const line = rawLine.trim();
      if (!line || line === "---") continue;
      const headingMatch = line.match(/^#{2,4}\s+(.+)/);
      if (headingMatch) {
        const title = headingMatch[1].trim();
        if (title.toLowerCase().includes("verificación")) {
          current = null;
          continue;
        }
        const meta = classifyHeading(title);
        current = { category: meta.category, label: meta.label, items: [] };
        sections.push(current);
        continue;
      }
      const bulletMatch = line.match(/^[-*•]\s+(.+)/);
      if (bulletMatch) {
        if (!current) {
          current = { category: "added", label: "Cambios", items: [] };
          sections.push(current);
        }
        current.items.push(bulletMatch[1].replace(/\*\*/g, "").trim());
      }
    }
    return sections.filter((s) => s.items.length > 0);
  }

  function normalizeGitHubRelease(ghRel, index, catalogRelease) {
    const tag = ghRel.tag_name || `v${index + 1}.0`;
    const parsedSections = parseMarkdownSections(ghRel.body || "");
    const sections = parsedSections.length > 0
      ? parsedSections
      : (catalogRelease && catalogRelease.sections) || [
          {
            category: "added",
            label: "Notas de la release",
            items: [ghRel.body ? ghRel.body.slice(0, 240) : "Release publicada en GitHub."],
          },
        ];

    const assets = (ghRel.assets || []).map((asset) => ({
      name: asset.name,
      url: asset.browser_download_url,
      size: formatBytes(asset.size),
      downloads: asset.download_count || 0,
      kind: asset.name.endsWith(".apk")
        ? "apk"
        : asset.name.endsWith(".aab")
          ? "aab"
          : asset.name.endsWith(".sha256")
            ? "sha256"
            : "file",
    }));

    return {
      tag,
      title: ghRel.name || `Flora ${tag}`,
      date: (ghRel.published_at || ghRel.created_at || "").slice(0, 10),
      isLatest: index === 0 && !ghRel.prerelease,
      prerelease: Boolean(ghRel.prerelease),
      source: "github",
      htmlUrl: ghRel.html_url,
      summary:
        (catalogRelease && catalogRelease.summary) ||
        `Publicación oficial ${tag} disponible para descarga directa desde GitHub Releases.`,
      assets,
      sections,
    };
  }

  function normalizeLocalRelease(rel, owner, repo) {
    const baseReleaseUrl = `https://github.com/${owner}/${repo}/releases/tag/${encodeURIComponent(rel.tag)}`;
    return {
      tag: rel.tag,
      title: rel.title || `Flora ${rel.tag}`,
      date: rel.date,
      isLatest: Boolean(rel.isLatest),
      prerelease: Boolean(rel.prerelease),
      source: "catalog",
      htmlUrl: `https://github.com/${owner}/${repo}/releases`,
      tagUrl: baseReleaseUrl,
      summary: rel.summary,
      assets: (rel.assets || []).map((a) => ({
        name: a.name,
        url: `https://github.com/${owner}/${repo}/releases/download/${encodeURIComponent(rel.tag)}/${encodeURIComponent(a.name)}`,
        fallbackUrl: `https://github.com/${owner}/${repo}/releases`,
        size: a.label || "",
        downloads: null,
        kind: a.kind || "file",
      })),
      sections: rel.sections || [],
    };
  }

  function updateRepoLinks() {
    const repoBase = `https://github.com/${state.owner}/${state.repo}`;
    const repoInput = document.getElementById("repo-input");
    if (repoInput) repoInput.value = `${state.owner}/${state.repo}`;
    const repoLink = document.getElementById("github-repo-link");
    if (repoLink) repoLink.href = repoBase;
    const footerLink = document.getElementById("footer-releases-link");
    if (footerLink) footerLink.href = `${repoBase}/releases`;
    const changelogBtn = document.getElementById("hero-changelog-btn");
    if (changelogBtn) changelogBtn.href = `${repoBase}/blob/main/CHANGELOG.md`;
    const releasesBtn = document.getElementById("hero-releases-btn");
    if (releasesBtn) releasesBtn.href = `${repoBase}/releases`;
  }

  function renderHero() {
    const latest = state.releases[0];
    if (!latest) return;

    const repoBase = `https://github.com/${state.owner}/${state.repo}`;
    document.getElementById("hero-version-pill").textContent = latest.tag;
    document.getElementById("hero-title").textContent = latest.title;
    document.getElementById("hero-summary").textContent = latest.summary;
    document.getElementById("hero-date").textContent = formatDate(latest.date);

    const apkAsset = latest.assets.find((a) => a.kind === "apk");
    const apkBtn = document.getElementById("hero-apk-btn");
    apkBtn.href = latest.source === "github" && apkAsset ? apkAsset.url : `${repoBase}/releases`;
    apkBtn.textContent = latest.source === "github" && apkAsset
      ? `Descargar ${apkAsset.name}`
      : `Ver Releases (${latest.tag})`;

    const shaCmd = document.getElementById("sha-command");
    shaCmd.textContent = `sha256sum -c Flora-${latest.tag}.apk.sha256`;

    const assetsList = document.getElementById("hero-assets-list");
    assetsList.innerHTML = latest.assets
      .map((asset) => {
        const href = latest.source === "github" ? asset.url : (asset.fallbackUrl || asset.url);
        const meta = [asset.size, asset.downloads != null ? `${asset.downloads} descargas` : ""]
          .filter(Boolean)
          .join(" · ");
        return `
          <div class="asset-row">
            <div>
              <strong class="mono">${escapeHtml(asset.name)}</strong>
              ${meta ? `<div class="small-muted">${escapeHtml(meta)}</div>` : ""}
            </div>
            <a href="${escapeHtml(href)}" target="_blank" rel="noopener noreferrer">
              ${latest.source === "github" ? "Descargar ↗" : "Releases ↗"}
            </a>
          </div>
        `;
      })
      .join("");
  }

  function renderUpcoming() {
    const upcomingEl = document.getElementById("upcoming-section");
    const upcoming = state.catalog && state.catalog.upcoming;
    if (!upcoming) {
      upcomingEl.hidden = true;
      return;
    }
    upcomingEl.hidden = false;
    const itemsHtml = (upcoming.sections || [])
      .map((sec) => {
        const cls = `cat-${sec.category}`;
        const list = sec.items.map((i) => `<li>${escapeHtml(i)}</li>`).join("");
        return `
          <div class="change-group">
            <span class="cat-badge ${cls}">${escapeHtml(sec.label)}</span>
            <ul class="change-list">${list}</ul>
          </div>
        `;
      })
      .join("");

    upcomingEl.innerHTML = `
      <div class="eyebrow-row">
        <span class="eyebrow">PRÓXIMA ETIQUETA · EN DESARROLLO</span>
        <span class="mono-badge">${escapeHtml(upcoming.tag)}</span>
      </div>
      <h3 class="release-title">${escapeHtml(upcoming.title)}</h3>
      <p class="small-muted">${escapeHtml(upcoming.summary)}</p>
      <div class="change-groups">${itemsHtml}</div>
    `;
  }

  function renderTimeline() {
    const container = document.getElementById("releases-timeline");
    const emptyEl = document.getElementById("empty-state");
    const countEl = document.getElementById("release-count");
    const q = state.searchQuery.trim().toLowerCase();
    const cat = state.activeCategory;

    const filtered = state.releases
      .map((rel) => {
        const matchingSections = (rel.sections || [])
          .filter((sec) => cat === "all" || sec.category === cat)
          .map((sec) => ({
            ...sec,
            items: q
              ? sec.items.filter(
                  (item) =>
                    item.toLowerCase().includes(q) ||
                    rel.tag.toLowerCase().includes(q) ||
                    rel.title.toLowerCase().includes(q)
                )
              : sec.items,
          }))
          .filter((sec) => sec.items.length > 0);

        const matchesHeader =
          !q ||
          rel.tag.toLowerCase().includes(q) ||
          rel.title.toLowerCase().includes(q) ||
          (rel.summary || "").toLowerCase().includes(q);

        if (cat === "all" && matchesHeader && matchingSections.length === 0) {
          return { ...rel, visibleSections: rel.sections || [] };
        }
        if (matchingSections.length > 0) {
          return { ...rel, visibleSections: matchingSections };
        }
        return null;
      })
      .filter(Boolean);

    countEl.textContent = `${filtered.length} ${filtered.length === 1 ? "versión" : "versiones"}`;

    if (filtered.length === 0) {
      container.innerHTML = "";
      emptyEl.hidden = false;
      return;
    }

    emptyEl.hidden = true;
    container.innerHTML = filtered
      .map((rel) => {
        const badges = [
          `<span class="mono-badge ${rel.isLatest ? "accent-terracotta" : ""}">${escapeHtml(rel.tag)}</span>`,
          rel.isLatest ? `<span class="mono-badge">Última</span>` : "",
          rel.prerelease ? `<span class="mono-badge">Pre-release</span>` : "",
          rel.source === "github"
            ? `<span class="mono-badge">GitHub Release</span>`
            : `<span class="mono-badge">Catálogo base</span>`,
        ]
          .filter(Boolean)
          .join(" ");

        const groupsHtml = rel.visibleSections
          .map(
            (sec) => `
              <div class="change-group">
                <span class="cat-badge cat-${escapeHtml(sec.category)}">${escapeHtml(sec.label)}</span>
                <ul class="change-list">
                  ${sec.items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}
                </ul>
              </div>
            `
          )
          .join("");

        const assetsHtml = (rel.assets || [])
          .map((asset) => {
            const href = rel.source === "github" ? asset.url : (asset.fallbackUrl || asset.url);
            return `
              <a class="asset-pill" href="${escapeHtml(href)}" target="_blank" rel="noopener noreferrer">
                <span class="mono">${escapeHtml(asset.name)}</span>
                ${asset.size ? `<span class="small-muted">${escapeHtml(asset.size)}</span>` : ""}
              </a>
            `;
          })
          .join("");

        return `
          <article class="plate release-card">
            <div class="release-card-head">
              <div>
                <div class="eyebrow-row">${badges}</div>
                <h3 class="release-title">${escapeHtml(rel.title)}</h3>
              </div>
              <div class="header-actions">
                <span class="mono small-muted">${escapeHtml(formatDate(rel.date))}</span>
                <a class="outline-btn small-btn" href="${escapeHtml(rel.htmlUrl)}" target="_blank" rel="noopener noreferrer">
                  Ver en GitHub ↗
                </a>
              </div>
            </div>
            <p class="release-summary">${escapeHtml(rel.summary)}</p>
            <div class="change-groups">${groupsHtml}</div>
            ${assetsHtml ? `<div class="release-assets-bar">${assetsHtml}</div>` : ""}
          </article>
        `;
      })
      .join("");
  }

  function renderScreenshots() {
    const grid = document.getElementById("screenshots-grid");
    const shots = (state.catalog && state.catalog.screenshots) || [];
    grid.innerHTML = shots
      .map(
        (shot) => `
          <figure class="shot-card" data-caption="${escapeHtml(shot.caption)}">
            <img
              src="./assets/${escapeHtml(shot.file)}"
              alt="${escapeHtml(shot.caption)}"
              loading="lazy"
              onerror="if (!this.dataset.triedFallback) { this.dataset.triedFallback='1'; this.src='${escapeHtml(shot.fallback)}'; }"
            />
            <figcaption>${escapeHtml(shot.caption)}</figcaption>
          </figure>
        `
      )
      .join("");

    grid.querySelectorAll(".shot-card").forEach((card) => {
      card.addEventListener("click", () => {
        const img = card.querySelector("img");
        const lb = document.getElementById("lightbox");
        document.getElementById("lightbox-img").src = img.currentSrc || img.src;
        document.getElementById("lightbox-img").alt = img.alt;
        document.getElementById("lightbox-caption").textContent = card.dataset.caption || "";
        lb.hidden = false;
      });
    });
  }

  async function loadData() {
    const badge = document.getElementById("sync-badge");
    const badgeText = document.getElementById("sync-badge-text");
    updateRepoLinks();

    try {
      const localRes = await fetch("./releases.json");
      if (localRes.ok) {
        state.catalog = await localRes.json();
      }
    } catch (_) {
      // Ignored; handled cleanly below
    }

    const catalogByTag = new Map();
    const localReleases = ((state.catalog && state.catalog.releases) || []).map((rel) => {
      catalogByTag.set(rel.tag, rel);
      return normalizeLocalRelease(rel, state.owner, state.repo);
    });

    try {
      const ghRes = await fetch(`https://api.github.com/repos/${state.owner}/${state.repo}/releases`, {
        headers: { Accept: "application/vnd.github+json" },
      });
      if (ghRes.ok) {
        const ghList = await ghRes.json();
        if (Array.isArray(ghList) && ghList.length > 0) {
          const mapped = ghList.map((ghRel, idx) =>
            normalizeGitHubRelease(ghRel, idx, catalogByTag.get(ghRel.tag_name))
          );
          const ghTags = new Set(mapped.map((r) => r.tag));
          const extraLocal = localReleases.filter((r) => !ghTags.has(r.tag));
          state.releases = [...mapped, ...extraLocal];
          badge.classList.add("live");
          badgeText.textContent = `GitHub API · ${ghList.length} release(s)`;
        } else {
          state.releases = localReleases;
          badge.classList.remove("live");
          badgeText.textContent = "Catálogo local · Publica un tag v*";
        }
      } else {
        state.releases = localReleases;
        badge.classList.remove("live");
        badgeText.textContent = "Catálogo local · Listo para publicar";
      }
    } catch (_) {
      state.releases = localReleases;
      badge.classList.remove("live");
      badgeText.textContent = "Modo sin conexión · Catálogo local";
    }

    renderHero();
    renderUpcoming();
    renderTimeline();
    renderScreenshots();
  }

  function initControls() {
    const savedTheme = localStorage.getItem("flora_pages_theme");
    if (savedTheme === "dark") {
      document.documentElement.setAttribute("data-theme", "dark");
    }

    const syncThemeUi = () => {
      const isDark = document.documentElement.getAttribute("data-theme") === "dark";
      document.getElementById("theme-icon").textContent = isDark ? "☾" : "☀";
      document.getElementById("theme-label").textContent = isDark ? "Papel" : "Nocturno";
    };
    syncThemeUi();

    document.getElementById("theme-toggle").addEventListener("click", () => {
      const isDark = document.documentElement.getAttribute("data-theme") === "dark";
      const next = isDark ? "light" : "dark";
      document.documentElement.setAttribute("data-theme", next);
      localStorage.setItem("flora_pages_theme", next);
      syncThemeUi();
    });

    document.getElementById("search-input").addEventListener("input", (e) => {
      state.searchQuery = e.target.value;
      renderTimeline();
    });

    document.querySelectorAll(".filter-chip").forEach((chip) => {
      chip.addEventListener("click", () => {
        document.querySelectorAll(".filter-chip").forEach((c) => c.classList.remove("active"));
        chip.classList.add("active");
        state.activeCategory = chip.dataset.category || "all";
        renderTimeline();
      });
    });

    document.getElementById("repo-form").addEventListener("submit", (e) => {
      e.preventDefault();
      const raw = document.getElementById("repo-input").value.trim();
      if (raw.includes("/")) {
        const [owner, repo] = raw.split("/");
        state.owner = owner.trim();
        state.repo = repo.trim();
        loadData();
      }
    });

    document.querySelectorAll("[data-copy-target]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const target = document.getElementById(btn.dataset.copyTarget);
        if (!target) return;
        const orig = btn.textContent;
        try {
          await navigator.clipboard.writeText(target.textContent);
          btn.textContent = "Copiado";
          setTimeout(() => {
            btn.textContent = orig;
          }, 1600);
        } catch (_) {
          btn.textContent = "Error";
        }
      });
    });

    const lb = document.getElementById("lightbox");
    document.getElementById("lightbox-close").addEventListener("click", () => {
      lb.hidden = true;
    });
    lb.addEventListener("click", (e) => {
      if (e.target === lb) lb.hidden = true;
    });
  }

  const detected = detectGitHubRepo();
  state.owner = detected.owner;
  state.repo = detected.repo;
  initControls();
  loadData();
})();

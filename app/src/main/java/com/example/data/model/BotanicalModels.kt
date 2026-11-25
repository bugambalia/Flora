package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.R

enum class BotanicalSpeciesId(val id: String) {
  TIGER_LILY("tiger_lily"),
  MONSTERA("monstera"),
  GINKGO("ginkgo"),
  LAVENDER("lavender"),
  LOTUS("lotus"),
  FERN("fern"),
  PASSIFLORA("passiflora"),
  CAMELLIA("camellia");

  companion object {
    fun fromId(id: String?): BotanicalSpeciesId =
      entries.firstOrNull { it.id == id } ?: TIGER_LILY
  }
}

enum class BotanicalPlayerTheme(
  val id: String,
  val displayName: String,
  val subtitle: String,
  @param:DrawableRes val backdropResId: Int,
  val baseForestColor: Color,
  val deepForestColor: Color,
  val glowColor: Color,
  val accentColor: Color,
) {
  ORCHIDS_WATERCOLOR(
    id = "orchids_watercolor",
    displayName = "Orquídeas Acuarela",
    subtitle = "Bosque profundo y oro suave",
    backdropResId = R.drawable.flora_orchids_watercolor,
    baseForestColor = Color(0xFF204235),
    deepForestColor = Color(0xFF132920),
    glowColor = Color(0xFFB8442C),
    accentColor = Color(0xFFE6C678),
  ),
  POPPIES_COBALT(
    id = "poppies_cobalt",
    displayName = "Amapolas y Cobalto",
    subtitle = "Azul real y pan de oro",
    backdropResId = R.drawable.flora_poppies_cobalt,
    baseForestColor = Color(0xFF18294A),
    deepForestColor = Color(0xFF0F192E),
    glowColor = Color(0xFFB8442C),
    accentColor = Color(0xFFE6C678),
  ),
  CHRYSANTHEMUM_MOON(
    id = "chrysanthemum_moon",
    displayName = "Luna y Crisantemo",
    subtitle = "Tinta nocturna y bermellón",
    backdropResId = R.drawable.flora_chrysanthemum_moon,
    baseForestColor = Color(0xFF143844),
    deepForestColor = Color(0xFF0C222A),
    glowColor = Color(0xFFC94132),
    accentColor = Color(0xFFF0DFB8),
  ),
  CANOPY_FOREST(
    id = "canopy_forest",
    displayName = "Dosel del Bosque",
    subtitle = "Verde umbrío de montaña",
    backdropResId = R.drawable.flora_canopy_forest,
    baseForestColor = Color(0xFF1D3C30),
    deepForestColor = Color(0xFF10241C),
    glowColor = Color(0xFF5D8A4E),
    accentColor = Color(0xFFDCE6B8),
  ),
  CATTLEYA_MOSAIC(
    id = "cattleya_mosaic",
    displayName = "Cattleya Azul Mosaico",
    subtitle = "Bosque andino y terracota",
    backdropResId = R.drawable.flora_cattleya_mosaic,
    baseForestColor = Color(0xFF233528),
    deepForestColor = Color(0xFF142118),
    glowColor = Color(0xFFB8442C),
    accentColor = Color(0xFFE6C678),
  ),
  LOTUS_POND(
    id = "lotus_pond",
    displayName = "Ciénaga Dorada",
    subtitle = "Loto fluvial y ocre cálido",
    backdropResId = R.drawable.flora_lotus_pond,
    baseForestColor = Color(0xFF193840),
    deepForestColor = Color(0xFF0F242A),
    glowColor = Color(0xFFC9786B),
    accentColor = Color(0xFFE6C678),
  ),
  SPIDER_LILY(
    id = "spider_lily",
    displayName = "Lirio Araña",
    subtitle = "Cobalto sereno y carmesí",
    backdropResId = R.drawable.flora_spider_lily,
    baseForestColor = Color(0xFF143C4A),
    deepForestColor = Color(0xFF0C242D),
    glowColor = Color(0xFFC93A2F),
    accentColor = Color(0xFFF3E7C9),
  ),
  BLUE_ANEMONES(
    id = "blue_anemones",
    displayName = "Anémonas Doradas",
    subtitle = "Índigo botánico y ocre",
    backdropResId = R.drawable.flora_blue_anemones,
    baseForestColor = Color(0xFF1B2C4A),
    deepForestColor = Color(0xFF111C30),
    glowColor = Color(0xFFD5A342),
    accentColor = Color(0xFFF2D68A),
  ),
  ENDEMIC_COLOMBIA(
    id = "endemic_colombia",
    displayName = "Flora Endémica",
    subtitle = "Páramo, palma de cera y niebla",
    backdropResId = R.drawable.flora_endemic_colombia,
    baseForestColor = Color(0xFF203A2E),
    deepForestColor = Color(0xFF12231B),
    glowColor = Color(0xFF839C74),
    accentColor = Color(0xFFE6C678),
  ),
  PROTEA_RAINFOREST(
    id = "protea_rainforest",
    displayName = "Selva Húmeda",
    subtitle = "Salvia silvestre y coral",
    backdropResId = R.drawable.flora_protea_rainforest,
    baseForestColor = Color(0xFF23382B),
    deepForestColor = Color(0xFF15231A),
    glowColor = Color(0xFFC95A3A),
    accentColor = Color(0xFFE6C678),
  ),
  BOTANICAL_ATLAS(
    id = "botanical_atlas",
    displayName = "Atlas Ilustrado",
    subtitle = "Lámina mixta de colección",
    backdropResId = R.drawable.flora_botanical_atlas,
    baseForestColor = Color(0xFF212D38),
    deepForestColor = Color(0xFF131B22),
    glowColor = Color(0xFFB8442C),
    accentColor = Color(0xFFE6C678),
  ),
  VINTAGE_ORCHIDS(
    id = "vintage_orchids",
    displayName = "Orquídeas Clásicas",
    subtitle = "Grabado naturalista nocturno",
    backdropResId = R.drawable.flora_orchids_vintage,
    baseForestColor = Color(0xFF1E362C),
    deepForestColor = Color(0xFF11211B),
    glowColor = Color(0xFFA94D6A),
    accentColor = Color(0xFFE6C678),
  );

  companion object {
    fun fromId(id: String?): BotanicalPlayerTheme? =
      entries.firstOrNull { it.id == id }

    fun forSpecies(speciesId: BotanicalSpeciesId): BotanicalPlayerTheme = when (speciesId) {
      BotanicalSpeciesId.TIGER_LILY -> SPIDER_LILY
      BotanicalSpeciesId.MONSTERA -> CANOPY_FOREST
      BotanicalSpeciesId.GINKGO -> BLUE_ANEMONES
      BotanicalSpeciesId.LAVENDER -> ORCHIDS_WATERCOLOR
      BotanicalSpeciesId.LOTUS -> LOTUS_POND
      BotanicalSpeciesId.FERN -> ENDEMIC_COLOMBIA
      BotanicalSpeciesId.PASSIFLORA -> CATTLEYA_MOSAIC
      BotanicalSpeciesId.CAMELLIA -> POPPIES_COBALT
    }
  }
}

@Immutable
data class AnatomicalCallout(
  val number: Int,
  val label: String,
  val latinTerm: String,
  val description: String,
  val xFraction: Float,
  val yFraction: Float,
  val labelOnLeft: Boolean = false,
)

@Immutable
data class BotanicalSpecies(
  val id: BotanicalSpeciesId,
  val commonName: String,
  val scientificName: String,
  val family: String,
  val nativeRegion: String,
  val coordinates: String,
  val soilPh: String,
  val heightSpec: String,
  val tempSpec: String,
  val humiditySpec: String,
  val associatedGenres: List<String>,
  val unlockConditionLabel: String,
  val description: String,
  val audioConnectionNote: String,
  val primaryColor: Color,
  val secondaryColor: Color,
  val foliageColor: Color,
  @param:DrawableRes val coverResId: Int,
  val callouts: List<AnatomicalCallout>,
)

object BotanicalCatalog {
  val allSpecies: List<BotanicalSpecies> = listOf(
    BotanicalSpecies(
      id = BotanicalSpeciesId.TIGER_LILY,
      commonName = "Lirio Tigre",
      scientificName = "Lilium lancifolium (L. tigrinum)",
      family = "Liliaceae",
      nativeRegion = "China, Japón, Corea y Lejano Oriente",
      coordinates = "36.2048° N, 138.25° E",
      soilPh = "pH : 5.5",
      heightSpec = "14.2 in. altura",
      tempSpec = "temp : 30° C",
      humiditySpec = "HR : 60%",
      associatedGenres = listOf("Folk", "Acústico", "Clásico"),
      unlockConditionLabel = "Espécimen inicial del herbario",
      description = "Lirio asiático perenne famoso por sus tépalos recurvados de color naranja ocre moteados con máculas púrpuras y bulbillos axilares oscuros que germinan directamente desde el tallo.",
      audioConnectionNote = "Sus armónicos cálidos evocan cuerdas acústicas y timbres de madera natural.",
      primaryColor = Color(0xFFB8442C),
      secondaryColor = Color(0xFF7D2E1D),
      foliageColor = Color(0xFF58614A),
      coverResId = R.drawable.flora_spider_lily,
      callouts = listOf(
        AnatomicalCallout(1, "Pistilo y estigma", "Gynoecium", "Órgano femenino central que sobresale más allá de la garganta floral.", 0.42f, 0.24f, true),
        AnatomicalCallout(2, "Estambres y anteras", "Androecium", "Seis filamentos largos con anteras pardo-rojizas cargadas de polen.", 0.31f, 0.34f, true),
        AnatomicalCallout(3, "Tépalos moteados", "Perianthium", "Pétalos y sépalos indistinguibles curvados hacia atrás en plena antesis.", 0.56f, 0.33f, false),
        AnatomicalCallout(4, "Bulbillos axilares", "Bulbillus", "Pequeños propágulos oscuros en la axila de las hojas lanceoladas.", 0.48f, 0.52f, true),
        AnatomicalCallout(5, "Corona y raíces", "Radix fibrosa", "Base subterránea escamosa de la cual emergen raíces adventicias.", 0.50f, 0.84f, false),
      )
    ),
    BotanicalSpecies(
      id = BotanicalSpeciesId.MONSTERA,
      commonName = "Costilla de Adán",
      scientificName = "Monstera deliciosa",
      family = "Araceae",
      nativeRegion = "Selvas húmedas del sur de México y Centroamérica",
      coordinates = "17.5046° N, 92.0436° O",
      soilPh = "pH : 6.0",
      heightSpec = "36.0 in. fronda",
      tempSpec = "temp : 26° C",
      humiditySpec = "HR : 82%",
      associatedGenres = listOf("Ambient", "Electrónica", "Lo-Fi"),
      unlockConditionLabel = "Reproduce música Ambient, Electrónica o Lo-Fi",
      description = "Trepadora hemiepífita de hojas coriáceas perforadas por fenestraciones naturales que permiten el paso de la luz y del viento monzónico hasta el sotobosque.",
      audioConnectionNote = "Las fenestraciones foliares reflejan la espacialidad y reverberación de paisajes sonoros ambientales.",
      primaryColor = Color(0xFF204235),
      secondaryColor = Color(0xFF173026),
      foliageColor = Color(0xFF58614A),
      coverResId = R.drawable.flora_canopy_forest,
      callouts = listOf(
        AnatomicalCallout(1, "Fenestración laminar", "Fenestra", "Aberturas elípticas en el limbo foliar que optimizan la captura luminosa.", 0.35f, 0.28f, true),
        AnatomicalCallout(2, "Nervadura central", "Costa media", "Vena gruesa y flexible que sostiene la amplia lámina tropical.", 0.52f, 0.36f, false),
        AnatomicalCallout(3, "Espádice y espata", "Spatha", "Inflorescencia cremosa protegida por una bráctea envolvente.", 0.64f, 0.48f, false),
        AnatomicalCallout(4, "Pecíolo acanalado", "Petiolus", "Tallo foliar largo con vaina basal resistente.", 0.48f, 0.62f, true),
        AnatomicalCallout(5, "Raíces aéreas", "Radix aeria", "Extensiones adventicias que absorben humedad ambiental y anclan la planta.", 0.55f, 0.82f, false),
      )
    ),
    BotanicalSpecies(
      id = BotanicalSpeciesId.GINKGO,
      commonName = "Ginkgo Biloba",
      scientificName = "Ginkgo biloba (Salisburia)",
      family = "Ginkgoaceae",
      nativeRegion = "Montes Tianmu, este de China",
      coordinates = "30.3350° N, 119.4400° E",
      soilPh = "pH : 6.4",
      heightSpec = "3.4 in. hoja",
      tempSpec = "temp : 18° C",
      humiditySpec = "HR : 55%",
      associatedGenres = listOf("Jazz", "Instrumental", "Clásico"),
      unlockConditionLabel = "Reproduce Jazz o Instrumental, o activa reproducción sin pausas",
      description = "Fósil viviente gimnospermo con más de 270 millones de años de historia. Sus hojas en abanico presentan venación dicotómica abierta que vira al oro puro en otoño.",
      audioConnectionNote = "Su simetría bifurcada simboliza la continuidad sin cortes del modo gapless.",
      primaryColor = Color(0xFFD5A342),
      secondaryColor = Color(0xFF9C7221),
      foliageColor = Color(0xFF7B8550),
      coverResId = R.drawable.flora_blue_anemones,
      callouts = listOf(
        AnatomicalCallout(1, "Lóbulo bilobado", "Lamella", "Margen superior hendido en dos secciones características en abanico.", 0.44f, 0.24f, true),
        AnatomicalCallout(2, "Venación dicotómica", "Nervatio", "Haces vasculares que se dividen de dos en dos sin formar red.", 0.60f, 0.34f, false),
        AnatomicalCallout(3, "Braquiblasto", "Brachyblastus", "Brote corto leñoso donde se agrupan de tres a cinco hojas.", 0.50f, 0.56f, true),
        AnatomicalCallout(4, "Semilla sarcotesta", "Semen", "Óvulo desnudo con cubierta carnosa dorada típica de las gimnospermas.", 0.62f, 0.72f, false),
      )
    ),
    BotanicalSpecies(
      id = BotanicalSpeciesId.LAVENDER,
      commonName = "Orquídea de Niebla",
      scientificName = "Cattleya azul mosaico",
      family = "Orchidaceae",
      nativeRegion = "Bosque andino y cordillera colombiana",
      coordinates = "4.6382° N, 75.5701° O",
      soilPh = "pH : 6.2",
      heightSpec = "18.5 in. espiga",
      tempSpec = "temp : 19° C",
      humiditySpec = "HR : 74%",
      associatedGenres = listOf("Chill", "Meditación", "Sueño"),
      unlockConditionLabel = "Activa el temporizador de reposo o reproduce música Chill",
      description = "Orquídea epífita de pétalos azul cobalto reticulados con labelo dorado que florece entre la bruma del bosque andino y conserva el rocío matinal.",
      audioConnectionNote = "Acompaña sesiones nocturnas de baja fatiga auditiva y temporizador de sueño.",
      primaryColor = Color(0xFF23479C),
      secondaryColor = Color(0xFF183270),
      foliageColor = Color(0xFF58614A),
      coverResId = R.drawable.flora_cattleya_mosaic,
      callouts = listOf(
        AnatomicalCallout(1, "Sépalos reticulados", "Perianthium", "Pétalos cerúleos con nervadura de mosaico mineral.", 0.50f, 0.22f, false),
        AnatomicalCallout(2, "Labelo dorado", "Labellum", "Pétalo central modificado que sirve de plataforma a polinizadores.", 0.40f, 0.36f, true),
        AnatomicalCallout(3, "Pseudobulbo", "Pseudobulbus", "Órgano de reserva hídrica adaptado a epífitas de niebla.", 0.36f, 0.58f, true),
        AnatomicalCallout(4, "Velamen radicular", "Velamen", "Tejido esponjoso plateado que captura humedad atmosférica.", 0.52f, 0.80f, false),
      )
    ),
    BotanicalSpecies(
      id = BotanicalSpeciesId.LOTUS,
      commonName = "Loto de Ciénaga",
      scientificName = "Nelumbo nucifera",
      family = "Nelumbonaceae",
      nativeRegion = "Ciénagas doradas y valles fluviales",
      coordinates = "9.2412° N, 74.7561° O",
      soilPh = "pH : 6.8",
      heightSpec = "28.0 in. pedúnculo",
      tempSpec = "temp : 29° C",
      humiditySpec = "HR : 88%",
      associatedGenres = listOf("Bossa", "Soul", "World"),
      unlockConditionLabel = "Reproduce Bossa, Soul o World, o selecciona un perfil acústico",
      description = "Planta acuática de hojas peltadas superhidrofóbicas y flores termorreguladas que abren sus pétalos marfil sobre espejos de agua dorados.",
      audioConnectionNote = "Su efecto hidrofóbico inspira la limpieza espectral sin distorsión.",
      primaryColor = Color(0xFFC96B7D),
      secondaryColor = Color(0xFF8C3B4D),
      foliageColor = Color(0xFF234B54),
      coverResId = R.drawable.flora_lotus_pond,
      callouts = listOf(
        AnatomicalCallout(1, "Receptáculo cónico", "Receptaculum", "Centro esponjoso dorado donde maduran los aquenios longevos.", 0.50f, 0.27f, false),
        AnatomicalCallout(2, "Pétalos cóncavos", "Corolla", "Capas imbricadas rosadas que se abren al alba.", 0.34f, 0.34f, true),
        AnatomicalCallout(3, "Hoja peltada", "Folium peltatum", "Lámina circular con microcristales cerosos que repelen el agua.", 0.68f, 0.58f, false),
        AnatomicalCallout(4, "Rizoma acuático", "Rhizoma", "Tallo sumergido con cámaras de aire longitudinales.", 0.46f, 0.83f, true),
      )
    ),
    BotanicalSpecies(
      id = BotanicalSpeciesId.FERN,
      commonName = "Palma y Helecho Endémico",
      scientificName = "Ceroxylon quindiuense",
      family = "Dryopteridaceae",
      nativeRegion = "Valle de Cocora y bosques nublados",
      coordinates = "4.6375° N, 75.4892° O",
      soilPh = "pH : 5.2",
      heightSpec = "24.0 in. fronda",
      tempSpec = "temp : 16° C",
      humiditySpec = "HR : 78%",
      associatedGenres = listOf("Indie", "Rock", "Bosque"),
      unlockConditionLabel = "Crea tu primera lista personalizada o reproduce Indie/Rock",
      description = "Flora ancestral de montaña que despliega frondas pinnadas entre la bruma del valle y resguarda la diversidad biológica de las laderas húmedas.",
      audioConnectionNote = "Su espiral de Fibonacci refrenda el orden y la curaduría de tus listas.",
      primaryColor = Color(0xFF3E6B48),
      secondaryColor = Color(0xFF204235),
      foliageColor = Color(0xFF58614A),
      coverResId = R.drawable.flora_endemic_colombia,
      callouts = listOf(
        AnatomicalCallout(1, "Ápice acuminado", "Apex frondis", "Extremo pinnado que capta luz filtrada bajo el dosel.", 0.50f, 0.19f, false),
        AnatomicalCallout(2, "Báculo circinado", "Vernatio", "Brote joven enrollado en espiral protectora antes de desplegarse.", 0.35f, 0.42f, true),
        AnatomicalCallout(3, "Pinnas y soros", "Sorus", "Segmentos foliares con cápsulas de esporas en el envés.", 0.64f, 0.50f, false),
        AnatomicalCallout(4, "Raquis escamoso", "Rachis", "Eje central cubierto de páleas pardo-doradas.", 0.48f, 0.72f, true),
      )
    ),
    BotanicalSpecies(
      id = BotanicalSpeciesId.PASSIFLORA,
      commonName = "Heliconia y Pasiflora",
      scientificName = "Heliconia rostrata",
      family = "Heliconiaceae",
      nativeRegion = "Selva húmeda tropical de América",
      coordinates = "5.0689° N, 75.5174° O",
      soilPh = "pH : 6.3",
      heightSpec = "3.8 in. bráctea",
      tempSpec = "temp : 25° C",
      humiditySpec = "HR : 76%",
      associatedGenres = listOf("Latina", "Pop", "Ritmo"),
      unlockConditionLabel = "Ajusta el ecualizador o reproduce Ritmo/Pop/Latina",
      description = "Inflorescencia colgante de brácteas escarlata con ribetes dorados y hojas azul-verdosas que atraen colibríes en la espesura tropical.",
      audioConnectionNote = "Sus filamentos y brácteas rítmicas recuerdan un analizador espectral vivo.",
      primaryColor = Color(0xFFB8442C),
      secondaryColor = Color(0xFF23479C),
      foliageColor = Color(0xFF58614A),
      coverResId = R.drawable.flora_botanical_atlas,
      callouts = listOf(
        AnatomicalCallout(1, "Estigmas capitados", "Stigma", "Tres brazos superiores en forma de maza sobre el ovario central.", 0.50f, 0.25f, false),
        AnatomicalCallout(2, "Corona filamentosa", "Corona", "Anillo radiante de filamentos bicolores que atrae polinizadores.", 0.34f, 0.35f, true),
        AnatomicalCallout(3, "Bráctea escarlata", "Bractea", "Hoja modificada cerosa que protege las flores verdaderas.", 0.64f, 0.42f, false),
        AnatomicalCallout(4, "Zarcillo helicoidal", "Cirrhus", "Órgano espiralado sensible al tacto para trepar.", 0.34f, 0.64f, true),
      )
    ),
    BotanicalSpecies(
      id = BotanicalSpeciesId.CAMELLIA,
      commonName = "Amapola Dorada",
      scientificName = "Papaver rhoeas (Var. cobalt)",
      family = "Papaveraceae",
      nativeRegion = "Jardines de altura y herbarios clásicos",
      coordinates = "33.5902° N, 130.4017° E",
      soilPh = "pH : 5.6",
      heightSpec = "4.5 in. flor",
      tempSpec = "temp : 15° C",
      humiditySpec = "HR : 65%",
      associatedGenres = listOf("Nocturno", "Piano", "Favoritos"),
      unlockConditionLabel = "Marca 2 pistas como favoritas o reproduce Piano/Nocturno",
      description = "Floración bermellón con estambres dorados y follaje azul cobalto sobre pan de oro, símbolo clásico de la colección botánica nocturna.",
      audioConnectionNote = "Asociada a la intimidad armónica del piano solista y colecciones favoritas.",
      primaryColor = Color(0xFFB8442C),
      secondaryColor = Color(0xFF23479C),
      foliageColor = Color(0xFF204235),
      coverResId = R.drawable.flora_poppies_cobalt,
      callouts = listOf(
        AnatomicalCallout(1, "Corola papirácea", "Corolla", "Pétalos sedosos bermellón con mácula basal oscura.", 0.52f, 0.28f, false),
        AnatomicalCallout(2, "Estambres radiales", "Androecium", "Corona central de anteras doradas alrededor de la cápsula.", 0.42f, 0.36f, true),
        AnatomicalCallout(3, "Follaje pinnado azul", "Folium serratum", "Hojas recortadas teñidas con pigmento azul mineral.", 0.34f, 0.60f, true),
        AnatomicalCallout(4, "Capullo colgante", "Alabastrum", "Yema floral vellosa que se yergue al abrirse al alba.", 0.66f, 0.64f, false),
      )
    )
  )

  fun getById(id: BotanicalSpeciesId): BotanicalSpecies =
    allSpecies.firstOrNull { it.id == id } ?: allSpecies.first()

  fun matchSpeciesForGenre(genre: String): BotanicalSpeciesId {
    val g = genre.lowercase()
    return when {
      g.contains("ambient") || g.contains("electr") || g.contains("lo-fi") || g.contains("lofi") -> BotanicalSpeciesId.MONSTERA
      g.contains("jazz") || g.contains("instrument") -> BotanicalSpeciesId.GINKGO
      g.contains("chill") || g.contains("medit") || g.contains("sleep") || g.contains("sueño") -> BotanicalSpeciesId.LAVENDER
      g.contains("bossa") || g.contains("soul") || g.contains("world") -> BotanicalSpeciesId.LOTUS
      g.contains("indie") || g.contains("rock") || g.contains("bosque") -> BotanicalSpeciesId.FERN
      g.contains("latin") || g.contains("pop") || g.contains("ritmo") -> BotanicalSpeciesId.PASSIFLORA
      g.contains("piano") || g.contains("noct") -> BotanicalSpeciesId.CAMELLIA
      else -> BotanicalSpeciesId.TIGER_LILY
    }
  }
}

object BotanicalLyricsCatalog {
  private val versesBySpecies: Map<BotanicalSpeciesId, List<String>> = mapOf(
    BotanicalSpeciesId.TIGER_LILY to listOf(
      "Entre la niebla despierta el valle",
      "la palma roza la mañana",
      "y el río guarda entre sus piedras",
      "el canto verde de la montaña",
      "Vuelvo a encontrarme en tus caminos",
      "bajo la flor del guayacán",
      "llevo el rumor de la cordillera",
      "como una luz al caminar"
    ),
    BotanicalSpeciesId.MONSTERA to listOf(
      "Cruza la lluvia por las hojas rotas",
      "abriendo claros en el sotobosque",
      "cada gota mide el pulso lento",
      "de la espesura que respira",
      "Sube el musgo por la corteza tibia",
      "tejiendo sombra de cristal verde",
      "mientras el viento escribe acordes",
      "en la quietud del mediodía"
    ),
    BotanicalSpeciesId.GINKGO to listOf(
      "Cae en silencio la hoja en abanico",
      "oro antiguo de dos inviernos",
      "se abre el tallo en ramas gemelas",
      "como un compás sin interrupción",
      "Guardan los siglos su geometría",
      "en cada nervio paralelo y firme",
      "y el aire vuelve transparente",
      "sobre el jardín del monasterio"
    ),
    BotanicalSpeciesId.LAVENDER to listOf(
      "Sube la bruma del bosque andino",
      "tiñendo de azul cada pétalo alto",
      "duerme el polen en oro fino",
      "bajo la calma del atardecer",
      "Respira el aire notas serenas",
      "aroma limpio de montaña alta",
      "y se apacigua todo murmullo",
      "hasta el regreso del amanecer"
    ),
    BotanicalSpeciesId.LOTUS to listOf(
      "Flota en el agua la copa alba",
      "sin que el rocío logre tocarla",
      "gira la tarde sobre los juncos",
      "pintando ciénagas de color ocre",
      "Nace la música desde el estanque",
      "clara y redonda como una perla",
      "y cada pétalo abre un reflejo",
      "sobre la orilla silenciosa"
    ),
    BotanicalSpeciesId.FERN to listOf(
      "Se desenrolla el brote en espiral",
      "bajo la palma alta de Cocora",
      "cruzan las aves entre la neblina",
      "llevando polen de valle en valle",
      "Toda raíz conoce su tierra",
      "todo sendero vuelve a la fronda",
      "y en la espesura crece constante",
      "el canto vivo de la sierra"
    ),
    BotanicalSpeciesId.PASSIFLORA to listOf(
      "Brilla en la selva la bráctea roja",
      "como una llama entre hojas verdes",
      "danza el colibrí frente a la rama",
      "marcando el ritmo del trópico vivo",
      "Se abren corolas como campanas",
      "vibrando al sol de la tarde cálida",
      "y cada acorde sube ligero",
      "entre lianas y flores nuevas"
    ),
    BotanicalSpeciesId.CAMELLIA to listOf(
      "Sobre pan de oro se abre la amapola",
      "pétalo rojo, nervio de cobalto",
      "suenan las teclas en la penumbra",
      "como pisadas sobre hojas secas",
      "Guarda el invierno flores intactas",
      "cera encendida contra el silencio",
      "y cada nota queda suspendida",
      "como una lámina en el herbario"
    )
  )

  fun getLyricsForTrack(track: AudioTrack?): List<String> {
    if (track == null) return versesBySpecies[BotanicalSpeciesId.TIGER_LILY].orEmpty()
    return versesBySpecies[track.speciesId] ?: versesBySpecies[BotanicalSpeciesId.TIGER_LILY].orEmpty()
  }
}

enum class AppThemeMode(val label: String) {
  SYSTEM("Sistema"),
  LIGHT("Claro"),
  DARK("Oscuro");

  companion object {
    fun fromName(name: String?): AppThemeMode =
      entries.firstOrNull { it.name == name } ?: SYSTEM
  }
}

enum class LibraryFilterTab(val label: String) {
  ALL("Todas"),
  FAVORITES("Favoritos"),
  ALBUMS("Álbumes"),
  ARTISTS("Artistas"),
  MOST_PLAYED("Más escuchadas"),
  RECENT("Recientes"),
}

enum class LibrarySortOrder(val label: String) {
  TITLE_AZ("Título A–Z"),
  ARTIST("Artista A–Z"),
  ALBUM("Álbum A–Z"),
  DURATION("Duración"),
  RECENT("Más recientes"),
  MOST_PLAYED("Más escuchadas"),
}

@Immutable
data class AudioTrack(
  val id: Long,
  val uri: String,
  val title: String,
  val artist: String,
  val album: String,
  val genre: String,
  val durationMs: Long,
  val bitrateKbps: Int,
  val sampleRateHz: Int,
  val codec: String,
  val coverPath: String?,
  val speciesId: BotanicalSpeciesId,
  val playCount: Int,
  val isFavorite: Boolean,
  val isBundled: Boolean,
  val sourceFolder: String = "Herbario",
  val sourcePath: String = "herbarium://bundled",
  val dateAdded: Long = 0L,
  val lyrics: String? = null,
)

@Immutable
data class AlbumGroupSummary(
  val albumName: String,
  val artistName: String,
  val tracks: List<AudioTrack>,
  val coverTrack: AudioTrack,
) {
  val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

@Immutable
data class ArtistGroupSummary(
  val artistName: String,
  val albumCount: Int,
  val tracks: List<AudioTrack>,
  val coverTrack: AudioTrack,
) {
  val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

@Immutable
data class AudioSourceSummary(
  val folderName: String,
  val folderPath: String,
  val trackCount: Int,
  val longTrackCount: Int,
  val isPersistedFolder: Boolean = false,
)

@Immutable
data class Playlist(
  val id: Long,
  val name: String,
  val description: String,
  val speciesMotif: BotanicalSpeciesId,
  val createdAt: Long,
  val updatedAt: Long,
)

@Immutable
data class PlaylistWithTracks(
  val playlist: Playlist,
  val tracks: List<AudioTrack>,
) {
  val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

@Immutable
data class BotanicalProgress(
  val speciesId: BotanicalSpeciesId,
  val isUnlocked: Boolean,
  val unlockedAt: Long?,
  val unlockReason: String?,
  val listenSeconds: Int,
)

@Immutable
data class AlbumPaletteColors(
  val dominant: Color = Color(0xFF204235),
  val vibrant: Color = Color(0xFFB8442C),
  val darkVibrant: Color = Color(0xFF173026),
  val lightVibrant: Color = Color(0xFFE6C678),
  val muted: Color = Color(0xFF58614A),
  val darkMuted: Color = Color(0xFF1D3C30),
  val onAccent: Color = Color(0xFFFFFAF0),
)

@Immutable
data class ThemeBackgroundGradients(
  val parchmentTop: Color = Color(0xFFF6EFE0),
  val parchmentBottom: Color = Color(0xFFEBE1CA),
  val parchmentRadialPrimary: Color = Color(0xFFD5A342).copy(alpha = 0.08f),
  val parchmentRadialSecondary: Color = Color(0xFFB8442C).copy(alpha = 0.09f),
  val heroGlowStart: Color = Color(0xFFB8442C).copy(alpha = 0.14f),
  val heroGlowEnd: Color = Color(0xFFD5A342).copy(alpha = 0.08f),
  val playerTop: Color = Color(0xFF204235),
  val playerMiddle: Color = Color(0xFF173026),
  val playerBottom: Color = Color(0xFF10221B),
  val playerGlow: Color = Color(0xFFB8442C),
  val miniPlayerStart: Color = Color(0xFF1D3C30),
  val miniPlayerEnd: Color = Color(0xFF264C3D),
) {
  companion object {
    val Default = ThemeBackgroundGradients()
  }
}

enum class EqualizerPreset(
  val label: String,
  val subtitle: String,
  val speciesId: BotanicalSpeciesId,
  val bandLevels: List<Int>,
  val recommendedBassBoost: Int,
  val recommendedVirtualizer: Int,
) {
  CUSTOM(
    label = "Personalizado",
    subtitle = "Curva acústica libre",
    speciesId = BotanicalSpeciesId.TIGER_LILY,
    bandLevels = listOf(220, -80, 120, -140, 380),
    recommendedBassBoost = 260,
    recommendedVirtualizer = 220,
  ),
  BOTANICAL_NATURAL(
    label = "Voz",
    subtitle = "Calidez orgánica y claridad vocal",
    speciesId = BotanicalSpeciesId.LOTUS,
    bandLevels = listOf(-80, 140, 320, 220, 110),
    recommendedBassBoost = 160,
    recommendedVirtualizer = 200,
  ),
  DEEP_ROOTS(
    label = "Bajos",
    subtitle = "Raíces profundas y cuerpo terroso",
    speciesId = BotanicalSpeciesId.MONSTERA,
    bandLevels = listOf(480, 310, 80, -40, 90),
    recommendedBassBoost = 720,
    recommendedVirtualizer = 280,
  ),
  FOREST_CLARITY(
    label = "Agudos",
    subtitle = "Brillo aéreo y detalle en cuerdas",
    speciesId = BotanicalSpeciesId.FERN,
    bandLevels = listOf(-90, 20, 150, 310, 440),
    recommendedBassBoost = 120,
    recommendedVirtualizer = 360,
  ),
  MONSTERA_CANOPY(
    label = "Dosel Monstera",
    subtitle = "Espacialidad húmeda de selva",
    speciesId = BotanicalSpeciesId.MONSTERA,
    bandLevels = listOf(300, 160, -40, 140, 280),
    recommendedBassBoost = 460,
    recommendedVirtualizer = 520,
  ),
  WARM_ACOUSTIC(
    label = "Oro de Ginkgo",
    subtitle = "Medios sedosos para jazz e instrumental",
    speciesId = BotanicalSpeciesId.GINKGO,
    bandLevels = listOf(100, 160, 180, 200, 140),
    recommendedBassBoost = 220,
    recommendedVirtualizer = 240,
  ),
  LAVENDER_NOCTURNE(
    label = "Niebla Andina",
    subtitle = "Agudos suaves sin fatiga auditiva",
    speciesId = BotanicalSpeciesId.LAVENDER,
    bandLevels = listOf(180, 110, 40, -140, -260),
    recommendedBassBoost = 200,
    recommendedVirtualizer = 320,
  ),
  LOTUS_RESONANCE(
    label = "Ciénaga Dorada",
    subtitle = "Claridad cristalina para bossa y folk",
    speciesId = BotanicalSpeciesId.LOTUS,
    bandLevels = listOf(200, 60, -70, 180, 320),
    recommendedBassBoost = 320,
    recommendedVirtualizer = 400,
  ),
  CLASSICAL_HERBARIUM(
    label = "Amapola de Cámara",
    subtitle = "Dinámica pura para piano y cuerdas",
    speciesId = BotanicalSpeciesId.CAMELLIA,
    bandLevels = listOf(160, 80, -30, 140, 260),
    recommendedBassBoost = 160,
    recommendedVirtualizer = 440,
  ),
}

@Immutable
data class AudioEffectsState(
  val equalizerEnabled: Boolean = true,
  val preset: EqualizerPreset = EqualizerPreset.CUSTOM,
  val bandLevelsMb: List<Int> = EqualizerPreset.CUSTOM.bandLevels,
  val bandCenterFreqsHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
  val minBandLevelMb: Int = -1000,
  val maxBandLevelMb: Int = 1000,
  val bassBoostStrength: Int = 260,
  val virtualizerStrength: Int = 220,
  val playbackSpeed: Float = 1.0f,
  val gaplessEnabled: Boolean = true,
  val crossfadeEnabled: Boolean = true,
  val chameleonThemeEnabled: Boolean = true,
  val playerThemeId: String = "AUTO",
  val sleepTimerMinutesRemaining: Int? = null,
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val minScanDurationSec: Int = 90,
)

enum class RepeatModeOption { OFF, ALL, ONE }

@Immutable
data class PlaybackState(
  val currentTrack: AudioTrack? = null,
  val queue: List<AudioTrack> = emptyList(),
  val currentIndex: Int = -1,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val bufferedPercentage: Int = 100,
  val shuffleEnabled: Boolean = false,
  val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
  val activePlaylistName: String? = null,
  val audioSessionId: Int = 0,
  val gaplessTransitionsCount: Int = 0,
  val errorMessage: String? = null,
)

@Immutable
sealed interface LibraryRowItem {
  val key: String
  val sectionChar: Char

  @Immutable
  data class SectionHeader(
    override val sectionChar: Char,
    val count: Int,
  ) : LibraryRowItem {
    override val key: String = "hdr_$sectionChar"
  }

  @Immutable
  data class TrackRow(
    val track: AudioTrack,
    val flatIndex: Int,
    override val sectionChar: Char,
  ) : LibraryRowItem {
    override val key: String = "tr_${track.id}"
  }
}

@Immutable
data class LibraryCatalogData(
  val sortedTracks: List<AudioTrack> = emptyList(),
  val rowItems: List<LibraryRowItem> = emptyList(),
  val availableLetters: Set<Char> = emptySet(),
  val letterToItemIndex: Map<Char, Int> = emptyMap(),
  val sectionTrackCounts: Map<Char, Int> = emptyMap(),
  val usesAlphabetSections: Boolean = true,
)

fun trackSectionChar(title: String): Char {
  val ch = title.trim().firstOrNull()?.uppercaseChar()
  return if (ch != null && ch in 'A'..'Z') ch else '#'
}

fun chSortPriority(ch: Char): Int = if (ch == '#') 1 else 0

fun formatListenDuration(seconds: Int): String {
  val safe = seconds.coerceAtLeast(0)
  val hours = safe / 3600
  val minutes = (safe % 3600) / 60
  val secs = safe % 60
  return when {
    hours > 0 -> "${hours}h ${minutes}m"
    minutes > 0 -> "${minutes}m ${secs}s"
    else -> "${secs}s"
  }
}

fun buildLibraryCatalogData(
  tracks: List<AudioTrack>,
  sortOrder: LibrarySortOrder = LibrarySortOrder.TITLE_AZ,
): LibraryCatalogData {
  if (tracks.isEmpty()) return LibraryCatalogData()

  val alphabeticalKeySelector: ((AudioTrack) -> String)? = when (sortOrder) {
    LibrarySortOrder.TITLE_AZ -> { it -> it.title }
    LibrarySortOrder.ARTIST -> { it -> it.artist }
    LibrarySortOrder.ALBUM -> { it -> it.album }
    LibrarySortOrder.DURATION,
    LibrarySortOrder.RECENT,
    LibrarySortOrder.MOST_PLAYED -> null
  }

  if (alphabeticalKeySelector == null) {
    val sorted = when (sortOrder) {
      LibrarySortOrder.DURATION -> tracks.sortedWith(
        compareByDescending<AudioTrack> { it.durationMs }.thenBy { it.title.trim().lowercase() }
      )
      LibrarySortOrder.RECENT -> tracks.sortedWith(
        compareByDescending<AudioTrack> { it.dateAdded }.thenByDescending { it.id }
      )
      LibrarySortOrder.MOST_PLAYED -> tracks.sortedWith(
        compareByDescending<AudioTrack> { it.playCount }.thenBy { it.title.trim().lowercase() }
      )
      else -> tracks
    }
    val rows = sorted.mapIndexed { idx, track ->
      LibraryRowItem.TrackRow(
        track = track,
        flatIndex = idx,
        sectionChar = trackSectionChar(track.title)
      )
    }
    return LibraryCatalogData(
      sortedTracks = sorted,
      rowItems = rows,
      availableLetters = emptySet(),
      letterToItemIndex = emptyMap(),
      sectionTrackCounts = emptyMap(),
      usesAlphabetSections = false,
    )
  }

  val sorted = tracks.sortedWith(
    compareBy<AudioTrack> { chSortPriority(trackSectionChar(alphabeticalKeySelector(it))) }
      .thenBy { alphabeticalKeySelector(it).trim().lowercase() }
      .thenBy { it.title.trim().lowercase() }
  )
  val rows = ArrayList<LibraryRowItem>(sorted.size + 26)
  val indexMap = HashMap<Char, Int>(32)
  val counts = HashMap<Char, Int>(32)
  val available = HashSet<Char>(32)

  var currentSection: Char? = null
  var sectionHeaderPos = -1
  var sectionCount = 0
  var flatIdx = 0

  for (track in sorted) {
    val secChar = trackSectionChar(alphabeticalKeySelector(track))
    if (secChar != currentSection) {
      if (currentSection != null && sectionHeaderPos >= 0) {
        rows[sectionHeaderPos] = LibraryRowItem.SectionHeader(currentSection, sectionCount)
        counts[currentSection] = sectionCount
      }
      currentSection = secChar
      available.add(secChar)
      indexMap[secChar] = rows.size
      sectionHeaderPos = rows.size
      sectionCount = 0
      rows.add(LibraryRowItem.SectionHeader(secChar, 0))
    }
    rows.add(LibraryRowItem.TrackRow(track = track, flatIndex = flatIdx, sectionChar = secChar))
    sectionCount++
    flatIdx++
  }
  if (currentSection != null && sectionHeaderPos >= 0) {
    rows[sectionHeaderPos] = LibraryRowItem.SectionHeader(currentSection, sectionCount)
    counts[currentSection] = sectionCount
  }

  return LibraryCatalogData(
    sortedTracks = sorted,
    rowItems = rows,
    availableLetters = available,
    letterToItemIndex = indexMap,
    sectionTrackCounts = counts,
    usesAlphabetSections = true,
  )
}

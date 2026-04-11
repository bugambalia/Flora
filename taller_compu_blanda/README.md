# Algoritmos Genéticos: Problema de la Mochila (KP)

Este repositorio contiene una implementación educativa de un **Algoritmo Genético (AG)** diseñado para resolver el **Problema de la Mochila (Knapsack Problem - KP)**. 

El proyecto utiliza herramientas modernas de Python como `uv` para la gestión de dependencias y entornos virtuales, garantizando una ejecución rápida y reproducible.

## 🚀 Inicio Rápido

### Requisitos Previos
- Tener instalado [uv](https://github.com/astral-sh/uv).

### Instalación
Sincroniza el entorno y las dependencias (incluyendo `numpy` y `rich`):
```bash
uv sync
```

### Ejecución
Para ejecutar la implementación principal del algoritmo genético:
```bash
uv run main.py
```

## 🛠️ Estructura del Proyecto

- `main.py`: Implementación central del Algoritmo Genético.
- `AGENTS.md`: Guía teórica, diseño experimental y hoja de ruta de la implementación.
- `docs.md`: Guía de uso de comandos `uv` y documentación técnica.
- `pyproject.toml`: Configuración del proyecto y dependencias (`numpy`, `rich`).

## 🧠 El Problema: Knapsack Problem (KP)
El objetivo es seleccionar un conjunto de objetos con pesos $w_i$ y beneficios $b_i$ que maximicen el beneficio total sin superar una capacidad $C$.

### Características del AG implementado:
- **Representación:** Cromosomas binarios.
- **Fitness:** Beneficio acumulado con **penalización por exceso de peso**.
- **Selección:** Métodos de Ruleta y Torneo.
- **Operadores:** Cruce (Crossover) de un punto y Mutación por inversión de bits.

## 📊 Diseño Experimental
El proyecto está diseñado para evaluar el rendimiento del algoritmo con diferentes tamaños de instancia (200, 500, 750 y 1000 ítems) y diferentes operadores de selección.

---
Desarrollado como parte del *Taller de Computación Blanda*.

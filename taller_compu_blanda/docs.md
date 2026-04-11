# Documentación Técnica: Algoritmo Genético para KP

Este proyecto implementa un AG para resolver el Problema de la Mochila.

## Parámetros del Algoritmo

Puedes ajustar los siguientes parámetros directamente en el archivo `main.py`:

- `m`: Número de objetos disponibles (ítems).
- `alpha`: Factor de capacidad ($C = \alpha \sum w_i$). Un valor de 0.5 significa que la mochila puede llevar la mitad del peso total disponible.
- `pop_size`: Tamaño de la población de individuos.
- `generations`: Número de ciclos evolutivos.
- `mutation_rate`: Probabilidad de que un bit sea invertido (ej. 0.01 para 1%).
- `crossover_rate`: Probabilidad de realizar un cruce entre dos padres.

## Métodos de Selección

El código soporta:
1. **Torneo (tournament)**: Elige el mejor de k individuos aleatorios.
2. **Ruleta (roulette)**: Selección proporcional al fitness.

## Cómo ejecutar experimentos

1. Sincroniza el entorno:
   ```bash
   uv sync
   ```
2. Ejecuta el algoritmo:
   ```bash
   uv run main.py
   ```

## Notas sobre la implementación

- **Penalización**: Si un individuo supera el peso permitido, su función de fitness se penaliza proporcionalmente al exceso de peso, desalentando su reproducción pero permitiendo que su material genético persista si es valioso.
- **Visualización**: Se utiliza la librería `rich` para mostrar progreso y resultados en una tabla clara.

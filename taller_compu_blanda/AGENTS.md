Para ayudarte con tu tarea de Algoritmos Genéticos (AG) aplicada al Problema de la Mochila (KP), he diseñado una estructura clara en Markdown. Este archivo `agents.md` servirá como hoja de ruta para tu implementación y el posterior informe.

---

# Implementación de Algoritmo Genético: Problema de la Mochila (KP)

Este documento detalla la estructura, parámetros y el diseño del experimento para resolver el problema de la mochila mediante Algoritmos Genéticos, siguiendo los requerimientos de la guía.

## 1. Definición del Problema

El objetivo es maximizar el beneficio total de los objetos seleccionados sin exceder la capacidad $C$ de la mochila.

* **Individuo:** Cadena binaria de longitud $m$ (donde 1 = incluido, 0 = excluido).
* **Fitness:** Suma de beneficios de los ítems, ajustada por una función de **penalización** si el peso total $W > C$.

---

## 2. Componentes del Algoritmo Genético

### Generación de Instancias

Se implementará la función `knapsack_instances(m, alpha)` con:

* $w_i \sim U[1, 20]$
* $b_i \sim U[10, 100]$
* $C = \alpha \sum_{i \in w} w_i$

### Operadores y Mecanismos

* **Penalización:** Las soluciones que excedan la capacidad verán su fitness reducido proporcionalmente al exceso de peso para evitar que dominen la población.
* **Selección:** * **Ruleta (pe-ru):** Selección proporcional al fitness.
* **Torneo (re-to):** Selección del mejor entre $k$ individuos aleatorios.


* **Crossover:** Cruce de un punto o dos puntos basado en el porcentaje de cruzamiento.
* **Mutación:** Inversión de bits según la tasa de mutación establecida.

---

## 3. Diseño Experimental

Para definir los valores óptimos de los parámetros, se completará la siguiente tabla de resultados:

| Items ($m$) | Operadores | Sol* (Mejor) | Evolution Plot | Time (s) |
| --- | --- | --- | --- | --- |
| 200 | pe-ru |  | [Gráfica 1] |  |
| 200 | pe-ru |  | [Gráfica 2] |  |
| 500 | re-to |  | [Gráfica 3] |  |
| 500 | pe-ru |  | [Gráfica 4] |  |
| 750 | * |  | [Gráfica 5] |  |
| 1000 | * |  | [Gráfica 6] |  |

> **Nota:** En los casos marcados con `*`, se recomienda probar configuraciones híbridas o ajustar la tasa de mutación para manejar el aumento de la dimensionalidad.

---

## 4. Estructura del Código Sugerida (Python)

```python
import numpy as np

def knapsack_instances(m, alpha):
    w = np.random.randint(1, 21, m)
    b = np.random.randint(10, 101, m)
    C = alpha * np.sum(w)
    return b, w, C

class GeneticAlgorithm:
    def __init__(self, population_size, mutation_rate, crossover_rate):
        # Inicialización de parámetros
        pass

    def fitness_function(self, individual, b, w, C):
        # Implementar penalización aquí
        pass

    def run(self):
        # Bucle principal de evolución
        pass

```

---

## 5. Próximos Pasos para el Informe

1. **Análisis de Convergencia:** Comparar cómo la selección por torneo vs. ruleta afecta la velocidad de convergencia.
2. **Sensibilidad de Parámetros:** Observar el impacto de la tasa de mutación en instancias grandes (1000 ítems).
3. **Conclusiones:** Determinar qué combinación de operadores fue más robusta para el problema KP.

---

¿Te gustaría que te ayude a redactar el código específico para la función de **penalización** o el método de **selección por ruleta**?
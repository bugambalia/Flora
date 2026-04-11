import numpy as np
from rich.console import Console
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, TaskProgressColumn
from rich.panel import Panel
import time

console = Console()

# --- Funciones de Configuración e Instancia ---

def crear_instancia_mochila(m: int, alpha: float):
    """Genera los datos del problema: beneficios, pesos y capacidad."""
    pesos = np.random.randint(1, 21, m)
    beneficios = np.random.randint(10, 101, m)
    capacidad = alpha * np.sum(pesos)
    return beneficios, pesos, capacidad

# --- Funciones del Núcleo del Algoritmo Genético ---

def calcular_fitness(individuo, b, w, C):
    """Evalúa qué tan buena es una solución aplicando penalización por exceso de peso."""
    peso_total = np.sum(individuo * w)
    beneficio_total = np.sum(individuo * b)

    if peso_total > C:
        # Penalización: se resta beneficio según el excedente
        penalizacion = (peso_total - C) * (np.max(b) / np.min(w))
        fitness = max(0, beneficio_total - penalizacion)
    else:
        fitness = beneficio_total

    return fitness, peso_total

def seleccionar_progenitor(poblacion, fitnesses, metodo="tournament", k=3):
    """Selecciona un individuo de la población para reproducirse."""
    if metodo == "tournament":
        indices = np.random.choice(len(fitnesses), k)
        ganador_idx = indices[np.argmax(fitnesses[indices])]
        return poblacion[ganador_idx]
    else:
        # Selección por Ruleta
        if np.sum(fitnesses) == 0:
            return poblacion[np.random.choice(len(poblacion))]
        probabilidades = fitnesses / np.sum(fitnesses)
        return poblacion[np.random.choice(len(poblacion"), p=probabilidades)]

def cruce_un_punto(padre1, padre2, tasa_cruce):
    """Combina dos padres para crear dos hijos."""
    m = len(padre1)
    if np.random.rand() < tasa_cruce:
        punto = np.random.randint(1, m)
        hijo1 = np.concatenate([padre1[:punto], padre2[punto:]])
        hijo2 = np.concatenate([padre2[:punto], padre1[punto:]])
        return hijo1, hijo2
    return padre1.copy(), padre2.copy()

def mutar_individuo(individuo, tasa_mutacion):
    """Invierte bits aleatoriamente según la tasa de mutación."""
    mascara = np.random.rand(len(individuo)) < tasa_mutacion
    individuo[mascara] = 1 - individuo[mascara]
    return individuo

# --- Función de Ejecución Principal (Orquestador) ---

def ejecutar_algoritmo_genetico(b, w, C, tam_poblacion=50, generaciones=100,
                               tasa_mut=0.01, tasa_cruce=0.8):
    """Lógica principal que evoluciona la población."""
    m = len(b)
    # Inicialización aleatoria
    poblacion = np.random.randint(0, 2, (tam_poblacion, m))

    mejor_historico = None
    mejor_fitness_val = -1

    for gen in range(generaciones):
        # Evaluación
        resultados = [calcular_fitness(ind, b, w, C) for ind in poblacion]
        fitnesses = np.array([res[0] for res in resultados])

        # Elitismo: guardar el mejor de la generación
        idx_max = np.argmax(fitnesses)
        if fitnesses[idx_max] > mejor_fitness_val:
            mejor_fitness_val = fitnesses[idx_max]
            mejor_historico = poblacion[idx_max].copy()

        # Construir nueva generación
        nueva_poblacion = []
        while len(nueva_poblacion) < tam_poblacion:
            p1 = seleccionar_progenitor(poblacion, fitnesses)
            p2 = seleccionar_progenitor(poblacion, fitnesses)

            h1, h2 = cruce_un_punto(p1, p2, tasa_cruce)

            nueva_poblacion.append(mutar_individuo(h1, tasa_mut))
            if len(nueva_poblacion) < tam_poblacion:
                nueva_poblacion.append(mutar_individuo(h2, tasa_mut))

        poblacion = np.array(nueva_poblacion)

    final_fit, final_peso = calcular_fitness(mejor_historico, b, w, C)
    return final_fit, final_peso

# --- Interfaz de Usuario ---

def main():
    console.clear()
    console.print(Panel("[bold green]Modo Funcional: Algoritmo Genético Aplicado[/]", expand=False))

    # Parámetros del experimento
    M, ALPHA = 200, 0.5
    POP, GENS = 50, 100

    beneficios, pesos, capacidad = crear_instancia_mochila(M, ALPHA)

    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        BarColumn(),
        TaskProgressColumn(),
        console=console,
    ) as progress:
        progress.add_task("[yellow]Procesando evolución...", total=None)

        # Llamada a la función principal
        resultado_fit, resultado_peso = ejecutar_algoritmo_genetico(
            beneficios, pesos, capacidad, POP, GENS
        )
        time.sleep(1) # Simulación para ver la barra

    # Presentación de resultados
    tabla = Table(title="Reporte de Optimización")
    tabla.add_column("Concepto", style="cyan")
    tabla.add_column("Resultado", style="bold white")

    tabla.add_row("Mejor Beneficio", f"{resultado_fit:.2f}")
    tabla.add_row("Peso Final / Capacidad", f"{resultado_peso:.2f} / {capacidad:.2f}")
    tabla.add_row("Eficiencia de Uso", f"{(resultado_peso/capacidad)*100:.2f}%")

    console.print(tabla)

if __name__ == "__main__":
    main()

import numpy as np
from rich.console import Console
from rich.table import Table
from rich.live import Live
from rich.panel import Panel
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, TaskProgressColumn
import time

console = Console()

def knapsack_instances(m: int, alpha: float):
    """
    Genera una instancia del problema de la mochila.
    - m: número de ítems.
    - alpha: factor para la capacidad de la mochila.
    """
    # Pesos entre 1 y 20
    w = np.random.randint(1, 21, m)
    # Beneficios entre 10 y 100
    b = np.random.randint(10, 101, m)
    # Capacidad proporcional a la suma total de pesos
    C = alpha * np.sum(w)
    return b, w, C

class GeneticAlgorithm:
    def __init__(self, population_size: int, m: int, mutation_rate: float, crossover_rate: float):
        self.population_size = population_size
        self.m = m
        self.mutation_rate = mutation_rate
        self.crossover_rate = crossover_rate
        # Inicialización de población aleatoria binaria
        self.population = np.random.randint(0, 2, (population_size, m))

    def fitness_function(self, individual, b, w, C):
        """
        Calcula el fitness con penalización si se excede la capacidad C.
        """
        total_weight = np.sum(individual * w)
        total_benefit = np.sum(individual * b)
        
        if total_weight > C:
            # Penalización: reducir beneficio proporcional al exceso de peso
            penalty = (total_weight - C) * (np.max(b) / np.min(w))
            fitness = max(0, total_benefit - penalty)
        else:
            fitness = total_benefit
            
        return fitness, total_weight

    def select_tournament(self, fitnesses, k=3):
        """Selección por torneo."""
        indices = np.random.choice(len(fitnesses), k)
        best_idx = indices[np.argmax(fitnesses[indices])]
        return self.population[best_idx]

    def select_roulette(self, fitnesses):
        """Selección por ruleta (proporcional)."""
        if np.sum(fitnesses) == 0:
            return self.population[np.random.choice(len(fitnesses))]
        probs = fitnesses / np.sum(fitnesses)
        idx = np.random.choice(len(fitnesses), p=probs)
        return self.population[idx]

    def crossover(self, parent1, parent2):
        """Cruce de un punto."""
        if np.random.rand() < self.crossover_rate:
            point = np.random.randint(1, self.m)
            child1 = np.concatenate([parent1[:point], parent2[point:]])
            child2 = np.concatenate([parent2[:point], parent1[point:]])
            return child1, child2
        return parent1.copy(), parent2.copy()

    def mutate(self, individual):
        """Mutación por inversión de bits."""
        mask = np.random.rand(self.m) < self.mutation_rate
        individual[mask] = 1 - individual[mask]
        return individual

    def run(self, b, w, C, generations=100, selection_method="tournament"):
        best_fitness_history = []
        best_individual = None
        current_best_fit = -1
        
        for gen in range(generations):
            # Evaluar población
            results = [self.fitness_function(ind, b, w, C) for ind in self.population]
            fitnesses = np.array([res[0] for res in results])
            weights = np.array([res[1] for res in results])
            
            # Guardar el mejor
            idx_max = np.argmax(fitnesses)
            if fitnesses[idx_max] > current_best_fit:
                current_best_fit = fitnesses[idx_max]
                best_individual = self.population[idx_max].copy()
            
            best_fitness_history.append(current_best_fit)
            
            # Nueva población
            new_population = []
            while len(new_population) < self.population_size:
                # Selección
                if selection_method == "tournament":
                    p1 = self.select_tournament(fitnesses)
                    p2 = self.select_tournament(fitnesses)
                else:
                    p1 = self.select_roulette(fitnesses)
                    p2 = self.select_roulette(fitnesses)
                
                # Cruce
                c1, c2 = self.crossover(p1, p2)
                
                # Mutación
                new_population.append(self.mutate(c1))
                if len(new_population) < self.population_size:
                    new_population.append(self.mutate(c2))
            
            self.population = np.array(new_population)
            
        final_fitness, final_weight = self.fitness_function(best_individual, b, w, C)
        return best_fitness_history, final_fitness, final_weight

def main():
    console.clear()
    console.print(Panel("[bold cyan]Algoritmo Genético - Problema de la Mochila (KP)[/]", expand=False))
    
    # Parámetros experimentales
    m = 200 # Número de ítems
    alpha = 0.5 # Factor de capacidad
    pop_size = 50
    generations = 100
    mutation_rate = 0.01
    crossover_rate = 0.8
    
    console.print(f"\n[yellow]Generando instancia de {m} ítems (alpha={alpha})...[/]")
    b, w, C = knapsack_instances(m, alpha)
    console.print(f"Capacidad de la mochila: [bold blue]{C:.2f}[/]\n")

    ga = GeneticAlgorithm(pop_size, m, mutation_rate, crossover_rate)
    
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        BarColumn(),
        TaskProgressColumn(),
        console=console,
    ) as progress:
        task1 = progress.add_task("[cyan]Evolucionando (Torneo)...", total=generations)
        
        # Simulación de ejecución con feedback visual
        best_history, final_fit, final_w = ga.run(b, w, C, generations, selection_method="tournament")
        
        for _ in range(generations):
            progress.update(task1, advance=1)
            time.sleep(0.01)

    # Mostrar resultados finales
    table = Table(title="Resultados Finales (Mejor Individuo)")
    table.add_column("Métrica", style="bold magenta")
    table.add_column("Valor", style="white")
    
    table.add_row("Mejor Fitness (Beneficio)", f"{final_fit:.2f}")
    table.add_row("Peso Total", f"{final_w:.2f} / {C:.2f}")
    table.add_row("Eficiencia (W/C)", f"{(final_w/C)*100:.1f}%")
    
    console.print(table)
    console.print("\n[dim]Nota: Revisa docs.md para instrucciones sobre cómo ajustar parámetros.[/]\n")

if __name__ == "__main__":
    main()

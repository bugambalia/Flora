import pygame
from pygame import Vector2

class Ball:
    def __init__(self, position, radius, color, screen, velocity)
        self.position : Vector2 = position
        self.radious = radius
        self.color = color
        self.screen = screen
        self.velocity = velocity

    def draw(self):
        pygame.draw.circle(self.screen, self.color, self.position, self.radious)

    def move(self):
        self.position += self.velocity

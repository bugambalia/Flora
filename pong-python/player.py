import pygame
from pygame import Vector2


class Player:
    def __init__(self, position: Vector2, color: str, size: Vector2, screen) -> None:
        super().__init__()
        self.position = position
        self.color = color
        self.screen = screen
        self.size = size

    def draw(self):
        pygame.draw.rect(
            self.screen,
            self.color,
            (self.position.x, self.position.y, self.size.x, self.size.y),
        )

    def move(self, keys, dt):
        if keys[pygame.K_w] and self.position.y > 0:
            self.position.y -= 300 * dt
        if keys[pygame.K_s]:
            self.position.y += 300 * dt
        if keys[pygame.K_a] and self.position.x > 0:
            self.position.x -= 300 * dt
        if keys[pygame.K_d]:
            self.position.x += 300 * dt

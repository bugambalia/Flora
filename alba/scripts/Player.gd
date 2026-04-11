#extends CharacterBody2D
#
#@export var speed = 300
#
#func _enter_tree():
	## El ID del peer que instanció este nodo será su autoridad
	## Importante: El nombre del nodo debe ser igual al ID del peer
	#set_multiplayer_authority(name.to_int())
#
#func _physics_process(_delta):
	## Solo el dueño del personaje puede moverlo
	#if is_multiplayer_authority():
		#var direction = Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down")
		#velocity = direction * speed
		#move_and_slide()
		#
		## Enviamos nuestra posición a los demás (Unreliable porque se actualiza constante)
		#sync_position.rpc(global_position)
#
#@rpc("any_peer", "unreliable")
#func sync_position(pos):
	#if not is_multiplayer_authority():
		#global_position = pos

extends CharacterBody2D

@export var speed: float = 100.0

@onready var sprite: AnimatedSprite2D = $AnimatedSprite2D

func _physics_process(delta):
	var direction = Vector2.ZERO

	# Entrada de movimiento
	direction.x = Input.get_action_strength("ui_right") - Input.get_action_strength("ui_left")
	direction.y = Input.get_action_strength("ui_down") - Input.get_action_strength("ui_up")

	direction = direction.normalized()

	# Movimiento
	velocity = direction * speed
	move_and_slide()

	# Animaciones
	update_animation(direction)


func update_animation(direction: Vector2):
	var anim = ""

	# Detectar dirección
	if direction == Vector2.ZERO:
		anim = get_idle_animation()
	else:
		anim = get_run_animation(direction)

	# Ataque (ejemplo con tecla)
	if Input.is_action_just_pressed("attack"):
		anim = get_attack_animation(direction)

	if sprite.animation != anim:
		sprite.play(anim)


func get_idle_animation() -> String:
	return "idle_down_right" # default


func get_run_animation(direction: Vector2) -> String:
	if direction.y > 0:
		return "run_down_left" if direction.x < 0 else "run_down_right"
	elif direction.y < 0:
		return "run_up_left" if direction.x < 0  else "run_up_right"
	else:
		return "run_down_left" if direction.x < 0 else "run_down_right"


func get_attack_animation(direction: Vector2) -> String:
	if direction == Vector2.ZERO:
		return "bite_down_right"

	if direction.y > 0:
		return "bite_down_left" if direction.x < 0 else "bite_down_right"
	elif direction.y < 0:
		return "bite_up_left" if direction.x < 0 else "bite_up_right"
	else:
		return "bite_down_left" if direction.x < 0  else "bite_down_right"

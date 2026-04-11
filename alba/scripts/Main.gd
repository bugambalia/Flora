extends Node2D

@export var player_scene: PackedScene = preload("res://Player.tscn")

func _ready():
	# Conectamos las señales de la MultiplayerAPI
	multiplayer.peer_connected.connect(add_player)
	multiplayer.peer_disconnected.connect(remove_player)

func add_player(id):
	print("Jugador conectado: ", id)
	var player = player_scene.instantiate()
	player.name = str(id) # El nombre coincide con el ID para la autoridad
	add_child(player)

func remove_player(id):
	if has_node(str(id)):
		get_node(str(id)).queue_free()

# Botones de UI para probar
func _on_host_pressed():
	Lobby.create_server()
	add_player(1) # El servidor siempre es ID 1

func _on_join_pressed():
	Lobby.join_game()

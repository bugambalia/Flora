extends Node

const PORT = 7000
const MAX_CLIENTS = 8

func create_server():
	var peer = ENetMultiplayerPeer.new()
	var error = peer.create_server(PORT, MAX_CLIENTS)
	if error != OK:
		print("Error al crear servidor: ", error)
		return
	multiplayer.multiplayer_peer = peer
	print("Servidor iniciado en el puerto ", PORT)

func join_game(ip = "127.0.0.1"):
	var peer = ENetMultiplayerPeer.new()
	var error = peer.create_client(ip, PORT)
	if error != OK:
		print("Error al conectar: ", error)
		return
	multiplayer.multiplayer_peer = peer
	print("Conectando a ", ip)

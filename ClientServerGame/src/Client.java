import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import game.InfoGame;
import game.Map;
import game.Utils;

public class Client {
	private Socket clientSocket ; // Stock le socket du client
	private ObjectOutputStream out; // Flux de sortie du serveur
	private ObjectInputStream in;// Flux de d'entré du serveur
	private InfoGame info; // Stock l'object InfoGame en donnée membre de client
	private Map map; // Stock l'object Map en donnée membre de client
	public Client() { // Constructeur du client qui lance la connexion
		connexion(); // Appel de la méthode connexion
	}
	public void connexion() { // Méthode connexion qui initialise la connexion
		try {
			/*
			 * les informations du serveur ( port et adresse IP ou nom d'hote
			 * 127.0.0.1 est l'adresse locale de la machine
			 */
			clientSocket = new Socket("127.0.0.1", 5000);
			out = new ObjectOutputStream(clientSocket.getOutputStream()); // Initialisation du flux de sortie
			in = new ObjectInputStream(clientSocket.getInputStream()); // Initialisation du flux d'entrée

		} catch (IOException e) {
			System.out.print("ERROR : "+e);
			System.exit(0);
		}
	}
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		Client client = new Client(); // Création d'une instance de Client
		Scanner sc = new Scanner(System.in); // Ouverture d'un flux Scanner
		System.out.println("Client connexion...");
		System.out.print("Entre votre nom : ");
		String nom = sc.nextLine();
		System.out.print("Entre votre classe : ");
		String classe = sc.nextLine();
		try {
			// Envoie du tableau de String vers le server
			client.out.writeObject(new String[] {"addPlayer", nom, classe, "+"});
			client.info = (InfoGame) client.in.readObject(); // Initialisation de la donnée membre info avec l'objet InfoGame recu du server
		} catch (IOException | ClassNotFoundException e) {
			System.out.print("ERROR : "+e);
		}
		client.map = Utils.viewMap(client.info.map); // Initialisation de la carte
		client.map.refresh(client.info); // Refresh de la carte
		while(true) { // Boucle principal
			try {
				String msg = (String) client.in.readObject(); // Réception de l'ordre du server
				if(msg.equals("Ready")) { // Si ready est envoi cela veut dire que c'est au tour du client
					System.out.print("Quelle action souhaitez-vous réaliser ? \n");
					String order = sc.nextLine();
					try {
						if(order.equals("save")) { // sauvagarde la partie
							client.out.writeObject(order);
							System.out.print("Entrez le nom de la sauvegarde ?\n");
							String filename = sc.nextLine();
							client.out.writeObject(filename);
						}else if(order.equals("load")) { // charge la partie
							client.out.writeObject(order);
							System.out.print("Quelle sauvegarde voulez-vous charger ?\n");
							String filename = sc.nextLine();
							client.out.writeObject(filename);
						}else if(order.equals("message")) { // envoi un message au serveur
							client.out.writeObject(order);
							System.out.print("Que souhaitez-vous dire ?\n");
							String msgtoserver = sc.nextLine();
							client.out.writeObject("Message de client : "+msgtoserver);
						}else { // sinon envoi l'ordre
							client.out.writeObject(order);
						}
					} catch (IOException e) {
						System.out.print("ERROR : "+e);
					}
				}else if(msg.equals("InfoGame")) { // Si le message est un message de mise a jour de l'etat du jeu
					try {
						client.info = (InfoGame) client.in.readObject();
						client.map.refresh(client.info);
					} catch (IOException | ClassNotFoundException e) {
						System.out.print("ERROR : "+e);
					}
				}else {
					System.out.println(msg);
				}
			} catch (ClassNotFoundException | IOException e) {
				System.out.print("ERROR : "+e);
			}
		}
	}
}
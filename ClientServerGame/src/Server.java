import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import game.Game;
import game.InfoGame;
import game.Map;
import game.Utils;

public class Server {
	private ServerSocket serveurSocket ; // Stock l'object socket du serveur
	private Socket clientSocket ; // Stock le socket du client connecté
	private ObjectOutputStream out; // Flux de sortie du serveur
	private ObjectInputStream in; // Flux de d'entré du serveur
	private int playerId; // ID du joueur connecté
	private int localId = 0; // ID du joueur local (donc le serveur)
	public Game g; // Donnée membre Game permet de garder l'objet Game à notre instance de Server
	public Map m; // Donnée membre Map permet de garder l'objet Map lié à notre instance de Server
	public Server() { // Constructeur de serveur vide
	}
	public String[] connexion() { // Méthode de connexion, retourne un tableau de string
		String[] msg = null; // Tableau de string utilisé pour recevoir les informations de connexion
		try {
			serveurSocket = new ServerSocket(5000); // Création d'un instance de ServerSocket sur le port 5000
			clientSocket = serveurSocket.accept(); // Accepte une connexion d'un client quand elle se présente
			out = new ObjectOutputStream(clientSocket.getOutputStream()); // Initialisation du flux de sortie
			in = new ObjectInputStream(clientSocket.getInputStream()); // Initialisation du flux d'entrée
			msg = (String[]) in.readObject(); // Reception des informations du client (AddPlayer, Nom, Classe, et caractère sur la carte)
		} catch (IOException | ClassNotFoundException e) {
			System.out.print("ERROR : "+e);
			System.exit(0);
		}
		return msg; // Retourne un tableau de string contenant les informations du client
	}

	public String saveGame(String filename, String gameType) { // Méthode de sauvegarde retournant un String, possède 2 paramètres, le nom du fichier pour la sauvegarde et le type de partie solo ou multijoueurs
		String msg = null; // Stock le message de retour de la méthode
		ObjectOutputStream saver; // Flux d'objet pour sauvegarder dans un dossier
		File f;
		if(gameType.equals("solo")) { // Verifie si la partie est solo ou multijoueurs et adapte en fonction
			f = new File("./data/save/solo");
		}else if(gameType.equals("multi")) {
			f = new File("./data/save/multi");
		}else {
			f = null;
			System.out.println("Erreur lors de la sauvegarde");
		}
		if(!f.exists()) { // Verifie l'existance des dossiers, si non, le crée.
			f.mkdir();
		}
		try {
			if(gameType.equals("solo")) { // Verifie si la partie est solo ou multijoueurs et adapte en fonction
				saver = new ObjectOutputStream(new FileOutputStream("./data/save/solo/"+filename+".sav"));
			}else if(gameType.equals("multi")) {
				saver = new ObjectOutputStream(new FileOutputStream("./data/save/multi/"+filename+".sav"));
			}else {
				saver = null;
				System.out.println("Erreur lors de la sauvegarde");
			}
			saver.writeObject(g); // Ecrit l'objet Game stocker dans la donnée membre g dans le fichier .sav
			saver.flush();
			saver.close();
			msg = "Sauvegarder effectuée avec succès";
		} catch (IOException e) {
			msg = "ERROR : "+e;
		}
		return msg; // Retourne le message de réussite/erreur
	}

	public String loadGame(String filename, String gameType) {// Méthode de chargement retournant un String, possède 2 paramètres, le nom du fichier pour la sauvegarde et le type de partie solo ou multijoueurs
		String msg = null; // Stock le message de retour de la méthode
		ObjectInputStream loader; // Flux d'objet pour charger les données d'un dossier 
		File f;		
		if(gameType.equals("solo")) {// Verifie si la partie est solo ou multijoueurs et adapte en fonction
			f = new File("./data/save/solo/"+filename+".sav");
		}else if(gameType.equals("multi")) {
			f = new File("./data/save/multi/"+filename+".sav");
		}else {
			f = null;
			System.out.println("Erreur lors du chargement");
		}

		if(f.exists()) { // Verifie si le fichier existe
			try {
				if(gameType.equals("solo")) {// Verifie si la partie est solo ou multijoueurs et adapte en fonction
					loader = new ObjectInputStream(new FileInputStream("./data/save/solo/"+filename+".sav"));
				}else if(gameType.equals("multi")) {
					loader = new ObjectInputStream(new FileInputStream("./data/save/multi/"+filename+".sav"));
				}else {
					loader = null;
					System.out.println("Erreur lors du chargement");
				}
				try {
					g = (Game) loader.readObject(); // Lit et Cast en objet Game les données contenu dans le fichier
				} catch (ClassNotFoundException e) {
					System.out.print("ERROR : "+e);
				}
				loader.close();
				msg = "Chargement effectuée avec succès";
			} catch (IOException e) {
				msg = "ERROR : "+e;
			}
		}else {
			msg = "Le fichier n'existe pas";
		}
		return msg;// Retourne le message de réussite/erreur
	}

	@SuppressWarnings("resource") // Je ne sais pas à quoi ça correspond exactement, mais évite les fuites de ressources du scanner non close.
	public static void main(String[] args) {
		Server serv = new Server(); // Création d'une instance de Server
		serv.g = new Game();// Création d'une instance de Game, stocké dans la donnée membre g de server
		serv.m = Utils.viewMap(serv.g.map); // Création d'une instance de Map, stocké dans la donnée membre m de server
		Scanner sc=new Scanner(System.in); // Ouverture d'un flux Scanner

		// Demande si le serveur se lance en mode solo ou multijoueurs
		System.out.print("Attentons t'on des joueurs ? (y / n) \n");
		String msg = sc.nextLine(); 
		if(msg.equals("y")) { // Version multijoueurs
			System.out.println("Serveur en attente d'un joueur");
			/* 
			 * Stocke dans la donnée membre playerId l'ID du joueurs
			 *	retourné par la méthode applyAddPlayer en lui passant en paramètre le retour de la méthode 
			 * connexion qui est pour rappel un tableau de String
			 */
			serv.playerId = serv.g.applyAddPlayer(serv.connexion());
			System.out.println("Connexion venant du client... (Player ID : "+serv.playerId+")");
			try {
				serv.out.writeObject(new InfoGame(serv.g, serv.g.joueurs.get(serv.playerId))); // Envoi de l'objet InfoGame au Client
			} catch (IOException e) {
				System.out.print("ERROR : "+e);
			}
			while(true) { // Boucle principal
				while(serv.g.heroTurn()) { // Première boucle secondaire
					try {
						System.out.print("Quelle action souhaitez-vous réaliser ? \n");
						String order = sc.nextLine();
						if(order.equals("save")) { // Entrée dans le mode sauvegarde
							System.out.print("Entrez le nom de la sauvegarde ?\n");
							String filename = sc.nextLine();
							System.out.println(serv.saveGame(filename, "multi"));
						}else if(order.equals("load")) {// Entrée dans le mode chargement
							System.out.print("Quelle sauvegarde voulez-vous charger ?\n");
							/*
							 * Liste toutes les sauvergarde, en récupérent dans lst_rep un tableau
							 * contenent le chemin abstrait de chaque fichier.
							 * Ensuite on appel la méthode getName de File qui retourne le nom du fichier
							 * a partir du chemin
							 */
							File[] lst_rep;
							lst_rep = new File("./data/save/multi").listFiles();
							for(File rep : lst_rep){
								if(rep.isFile())
								{ 
									System.out.println(rep.getName()); 
								} 
							}
							String filename = sc.nextLine();
							String text = serv.loadGame(filename, "multi"); // Charge la partie
							System.out.println(text);
							serv.out.writeObject(text);
							serv.out.writeObject("InfoGame");
							serv.out.writeObject(new InfoGame(serv.g, serv.g.joueurs.get(serv.playerId)));
							serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
						}else if(order.equals("message")) { // Permet de 'tchat' avec l'autre joueur
							System.out.print("Que souhaitez-vous dire ?\n");
							String msgtoplayer = sc.nextLine();
							serv.out.writeObject("Message de serveur : "+msgtoplayer);
						}else { // Sinon applique l'ordre de base
							String text = serv.g.applyOrder(order);
							System.out.println(text);
							serv.out.writeObject(text);
							serv.out.writeObject("InfoGame");
							serv.out.writeObject(new InfoGame(serv.g, serv.g.joueurs.get(serv.playerId)));
							serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
						}
					} catch (IOException e) {
						System.out.print("ERROR : "+e);
					}
				}
				System.out.println(serv.g.applySwitch());
				while(serv.g.heroTurn()) { // Deuxième boucle secondaire
					try {
						serv.out.writeObject("Ready");
						String playerOrder = (String) serv.in.readObject(); // Stock l'ordre reçu du joueur
						if(playerOrder.equals("message")) { // Recois un message du joueur
							System.out.println((String) serv.in.readObject());
						}else if (playerOrder.equals("save")) { // Sauvegarde la partie
							String filename = (String) serv.in.readObject(); // Reçois par le flux d'entré et caste en String
							System.out.println(serv.saveGame(filename, "multi"));
						}else if (playerOrder.equals("load")) { // Charge la partie
							String filename = (String) serv.in.readObject(); 
							String text = serv.loadGame(filename, "multi");
							System.out.println(text);
							serv.out.writeObject(text);
							serv.out.writeObject("InfoGame");
							serv.out.writeObject(new InfoGame(serv.g, serv.g.joueurs.get(serv.playerId)));
							serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
						}else { // Sinon applique l'ordre du joueur
							String text = serv.g.applyOrder(playerOrder);
							System.out.println(text);
							serv.out.writeObject(text);
							serv.out.writeObject("InfoGame");
							serv.out.writeObject(new InfoGame(serv.g, serv.g.joueurs.get(serv.playerId)));
							serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
						}
					} catch (IOException | ClassNotFoundException e) {
						System.out.print("ERROR : "+e);
					}
				}
				/*
				 * Déplacement des monstres et refresh de la carte.
				 */
				System.out.println(serv.g.applySwitch());
				System.out.println("Les monstres se déplacent");
				serv.g.monstersChase();
				serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
				try {
					serv.out.writeObject("Les monstres se déplacent");
					serv.out.writeObject("InfoGame");
					serv.out.writeObject(new InfoGame(serv.g, serv.g.joueurs.get(serv.playerId)));
				} catch (IOException e) {
					System.out.print("ERROR : "+e);
				}
			}
		}else{ // Si la partie est en solo
			while(true) { // Boucle principal
				while(serv.g.heroTurn()) { // Boucle secondaire
					System.out.print("Quelle action souhaitez-vous réaliser ? \n");
					String order = sc.nextLine();
					if(order.equals("save")) { // Sauvegarde de la partie
						System.out.print("Entrez le nom de la sauvegarde ?\n");
						String filename = sc.nextLine();
						System.out.println(serv.saveGame(filename, "solo"));
					}else if(order.equals("load")) { // Chargement de la partie
						System.out.print("Quelle sauvegarde voulez-vous charger ?\n");
						/*
						 * Liste toutes les sauvergarde, en récupérent dans lst_rep un tableau
						 * contenent le chemin abstrait de chaque fichier.
						 * Ensuite on appel la méthode getName de File qui retourne le nom du fichier
						 * à partir du chemin
						 */
						File[] lst_rep;
						lst_rep = new File("./data/save").listFiles();
						for(File rep : lst_rep){
							if(rep.isFile())
							{ 
								System.out.println(rep.getName()); 
							} 
						}
						String filename = sc.nextLine();
						String text = serv.loadGame(filename, "solo");
						System.out.println(text);
						serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
					}else {
						String text = serv.g.applyOrder(order);
						System.out.println(text);
						serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
					}
				}
				System.out.println(serv.g.applySwitch());
				System.out.println("Les monstres se déplacent");
				serv.g.monstersChase();
				serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
			}

		}
	}	
}

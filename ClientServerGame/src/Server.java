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
	private Socket clientSocket ; // Stock le socket du client connect�
	private ObjectOutputStream out; // Flux de sortie du serveur
	private ObjectInputStream in; // Flux de d'entr� du serveur
	private int playerId; // ID du joueur connect�
	private int localId = 0; // ID du joueur local (donc le serveur)
	public Game g; // Donn�e membre Game permet de garder l'objet Game � notre instance de Server
	public Map m; // Donn�e membre Map permet de garder l'objet Map li� � notre instance de Server
	public Server() { // Constructeur de serveur
		/*
		 * V�rifie si les dossiers pour les sauvegardes existes.
		 * C'est pas sur beau, mais �a a le merite de fonctionner plutot bien
		 */
		File f; 
		f = new File("./data/save");
		if(!f.exists()) { // Verifie l'existance des dossiers, si non, le cr�e.
			f.mkdir();
		}
		f = new File("./data/save/solo");
		if(!f.exists()) { // Verifie l'existance des dossiers, si non, le cr�e.
			f.mkdir();
		}
		f = new File("./data/save/multi");
		if(!f.exists()) { // Verifie l'existance des dossiers, si non, le cr�e.
			f.mkdir();
		}
	}
	public String[] connexion() { // M�thode de connexion, retourne un tableau de string
		String[] msg = null; // Tableau de string utilis� pour recevoir les informations de connexion
		try {
			serveurSocket = new ServerSocket(5000); // Cr�ation d'un instance de ServerSocket sur le port 5000
			clientSocket = serveurSocket.accept(); // Accepte une connexion d'un client quand elle se pr�sente
			out = new ObjectOutputStream(clientSocket.getOutputStream()); // Initialisation du flux de sortie
			in = new ObjectInputStream(clientSocket.getInputStream()); // Initialisation du flux d'entr�e
			msg = (String[]) in.readObject(); // Reception des informations du client (AddPlayer, Nom, Classe, et caract�re sur la carte)
		} catch (IOException | ClassNotFoundException e) {
			System.out.print("ERROR : "+e);
			System.exit(0);
		}
		return msg; // Retourne un tableau de string contenant les informations du client
	}

	public String saveGame(String filename, String gameType) { // M�thode de sauvegarde retournant un String, poss�de 2 param�tres, le nom du fichier pour la sauvegarde et le type de partie solo ou multijoueurs
		String msg = null; // Stock le message de retour de la m�thode
		ObjectOutputStream saver; // Flux d'objet pour sauvegarder dans un dossier
		try {
			if(gameType.equals("solo")) { // Verifie si la partie est solo ou multijoueurs et adapte en fonction
				saver = new ObjectOutputStream(new FileOutputStream("./data/save/solo/"+filename+".sav"));
			}else if(gameType.equals("multi")) {
				saver = new ObjectOutputStream(new FileOutputStream("./data/save/multi/"+filename+".sav"));
			}else {
				saver = null;
				System.out.println("Erreur lors de la sauvegarde");
			}
			saver.writeObject(g); // Ecrit l'objet Game stocker dans la donn�e membre g dans le fichier .sav
			saver.flush();
			saver.close();
			msg = "Sauvegarder effectu�e avec succ�s";
		} catch (IOException e) {
			msg = "ERROR : "+e;
		}
		return msg; // Retourne le message de r�ussite/erreur
	}

	public String loadGame(String filename, String gameType) {// M�thode de chargement retournant un String, poss�de 2 param�tres, le nom du fichier pour la sauvegarde et le type de partie solo ou multijoueurs
		String msg = null; // Stock le message de retour de la m�thode
		ObjectInputStream loader; // Flux d'objet pour charger les donn�es d'un dossier 
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
					g = (Game) loader.readObject(); // Lit et Cast en objet Game les donn�es contenu dans le fichier
				} catch (ClassNotFoundException e) {
					System.out.print("ERROR : "+e);
				}
				loader.close();
				msg = "Chargement effectu�e avec succ�s";
			} catch (IOException e) {
				msg = "ERROR : "+e;
			}
		}else {
			msg = "Le fichier n'existe pas";
		}
		return msg;// Retourne le message de r�ussite/erreur
	}

	@SuppressWarnings("resource") // Je ne sais pas � quoi �a correspond exactement, mais �vite les fuites de ressources du scanner non close.
	public static void main(String[] args) {
		Server serv = new Server(); // Cr�ation d'une instance de Server
		serv.g = new Game();// Cr�ation d'une instance de Game, stock� dans la donn�e membre g de server
		serv.m = Utils.viewMap(serv.g.map); // Cr�ation d'une instance de Map, stock� dans la donn�e membre m de server
		Scanner sc=new Scanner(System.in); // Ouverture d'un flux Scanner

		// Demande si le serveur se lance en mode solo ou multijoueurs
		System.out.print("Attentons t'on des joueurs ? (y / n) \n");
		String msg = sc.nextLine(); 
		if(msg.equals("y")) { // Version multijoueurs
			System.out.println("Serveur en attente d'un joueur");
			/* 
			 * Stocke dans la donn�e membre playerId l'ID du joueurs
			 *	retourn� par la m�thode applyAddPlayer en lui passant en param�tre le retour de la m�thode 
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
				while(serv.g.heroTurn()) { // Premi�re boucle secondaire
					try {
						System.out.print("Quelle action souhaitez-vous r�aliser ? \n");
						String order = sc.nextLine();
						if(order.equals("save")) { // Entr�e dans le mode sauvegarde
							System.out.print("Entrez le nom de la sauvegarde ?\n");
							String filename = sc.nextLine();
							System.out.println(serv.saveGame(filename, "multi"));
						}else if(order.equals("load")) {// Entr�e dans le mode chargement
							System.out.print("Quelle sauvegarde voulez-vous charger ?\n");
							/*
							 * Liste toutes les sauvergarde, en r�cup�rent dans lst_rep un tableau
							 * contenent le chemin abstrait de chaque fichier.
							 * Ensuite on appel la m�thode getName de File qui retourne le nom du fichier
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
				while(serv.g.heroTurn()) { // Deuxi�me boucle secondaire
					try {
						serv.out.writeObject("Ready");
						String playerOrder = (String) serv.in.readObject(); // Stock l'ordre re�u du joueur
						if(playerOrder.equals("message")) { // Recois un message du joueur
							System.out.println((String) serv.in.readObject());
						}else if (playerOrder.equals("save")) { // Sauvegarde la partie
							String filename = (String) serv.in.readObject(); // Re�ois par le flux d'entr� et caste en String
							System.out.println(serv.saveGame(filename, "multi"));
						}else if (playerOrder.equals("load")) { // Charge la partie
							serv.out.writeObject(new File("./data/save/multi").listFiles());
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
				 * D�placement des monstres et refresh de la carte.
				 */
				System.out.println(serv.g.applySwitch());
				System.out.println("Les monstres se d�placent");
				serv.g.monstersChase();
				serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
				try {
					serv.out.writeObject("Les monstres se d�placent");
					serv.out.writeObject("InfoGame");
					serv.out.writeObject(new InfoGame(serv.g, serv.g.joueurs.get(serv.playerId)));
				} catch (IOException e) {
					System.out.print("ERROR : "+e);
				}
			}
		}else{ // Si la partie est en solo
			while(true) { // Boucle principal
				while(serv.g.heroTurn()) { // Boucle secondaire
					System.out.print("Quelle action souhaitez-vous r�aliser ? \n");
					String order = sc.nextLine();
					if(order.equals("save")) { // Sauvegarde de la partie
						System.out.print("Entrez le nom de la sauvegarde ?\n");
						String filename = sc.nextLine();
						System.out.println(serv.saveGame(filename, "solo"));
					}else if(order.equals("load")) { // Chargement de la partie
						System.out.print("Quelle sauvegarde voulez-vous charger ?\n");
						/*
						 * Liste toutes les sauvergarde, en r�cup�rent dans lst_rep un tableau
						 * contenent le chemin abstrait de chaque fichier.
						 * Ensuite on appel la m�thode getName de File qui retourne le nom du fichier
						 * � partir du chemin
						 */
						File[] lst_rep;
						lst_rep = new File("./data/save/solo").listFiles();
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
				System.out.println("Les monstres se d�placent");
				serv.g.monstersChase();
				serv.m.refresh(new InfoGame(serv.g, serv.g.joueurs.get(serv.localId)));
			}

		}
	}	
}
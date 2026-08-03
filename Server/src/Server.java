/*
 * Classe principale del server che gestisce:
 * 1. L'avvio della connessione socket.
 * 2. Il caricamento delle domande da file.
 * 3. La gestione delle connessioni dei client, assegnando un ClientHandler per ogni utente.
 * 4. Domande, classifica, client attivi.
 * 5. La trasmissione (broadcast) della classifica aggiornata a tutti i client.
 * Quindi diciamo che si occupa in generale di gestire i dati, poi sarà ClientHandler a gestire meglio i singoli client
 * @author Simone Fusar Bassini e Andrea Cornetti
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    public static final int PORT = 1234; // orta di ascolto del Server.
    // liste statiche e globali accessibili da tutti i ClientHandler:
    public static List<Domanda> domande = new ArrayList<>(); //  lista che contiene tutte le domande del quiz.
    public static List<UtenteSessione> classifica = new ArrayList<>(); // lista della classifica
    public static List<ClientHandler> clientHandlers = new ArrayList<>(); // lista dei gestori (thread) dei client attivi.

    public static void main(String[] args) {
        caricaDomande("questions.txt"); // carica le domande dal file esterno.

        try (ServerSocket serverSocket = new ServerSocket(PORT)) { 
            System.out.println("Server avviato sulla porta " + PORT);

            // ciclo infinito per accettare continuamente nuove connessioni.
            while (true) {
                Socket socket = serverSocket.accept(); // accetta connessione
                ClientHandler handler = new ClientHandler(socket); //passa connessione al gestore del client
                
                /*
                la  lista globale dei client handler è synchronized, 
                poiché può essere modificata  da più thread contemporaneamente 
                (quando un client si connette o si disconnette).
                */
                synchronized (clientHandlers) {
                    clientHandlers.add(handler);
                }
                
                // avvia un nuovo thread per gestire il singolo client
                new Thread(handler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // metodo per caricare le domande dal file di testo
    public static void caricaDomande(String filename) { 
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                // parsing della riga: [Testo Domanda]|[Risposta Corretta]|[Punteggio]
                String[] parts = line.split("\\|");//per splittare per | , devo usare un regex
                if (parts.length == 3) {
                    domande.add(new Domanda(parts[0], parts[1], Integer.parseInt(parts[2])));
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file domande: " + e.getMessage());
        }
    }

    /* 
    Metodo per inviare la classifica corrente a tutti i client attivi.
     È synchronized per prevenire che più thread chiamino il broadcast contemporaneamente.
    */
    public static synchronized void broadcastClassifica() { 
        StringBuilder sb = new StringBuilder();
        // costruisce la stringa della classifica nel formato: "nome=punteggio;nome2=punteggio2;..."
        for (UtenteSessione u : classifica) {
            sb.append(u.nome).append("=").append(u.punteggio).append(";");
        }
        String classificaString = sb.toString();

        // ciclo sincronizzato per inviare a tutti i client handler.
        synchronized (clientHandlers) {
            // uso di Iterator per rimuovere in sicurezza i client disconnessi durante l'iterazione.
            Iterator<ClientHandler> iterator = clientHandlers.iterator();
            while (iterator.hasNext()) {
                ClientHandler client = iterator.next();
                try {
                    client.out.println("CLASSIFICA|" + classificaString);
                } catch (Exception e) {
                    // se l'invio fallisce (socket chiusa), rimuove l'handler dalla lista.
                    System.out.println("Client disconnesso durante il broadcast, rimosso.");
                    iterator.remove();
                }
            }
        }
    }
}
/*
 *classe che gestisce un singolo client
 * viene eseguita in un thread dedicato per consentire al server di gestire più utenti contemporaneamente.
 * @author Simone Fusar Bassini e Andrea Cornetti
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {

    Socket socket;
    PrintWriter out;
    BufferedReader in;
    
    public ClientHandler(Socket s) {
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true); //scrivi via socket
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));//leggi via socket

            //1)Legge il nnome
            String nome = in.readLine();

            //2)Prende domande dal server e le mette in ordine casuale
            List<Domanda> domandeRandom = new ArrayList<>(Server.domande);
            Collections.shuffle(domandeRandom);//ordina casuale
            List<String> risposteClient = new ArrayList<>();
            //3): fa domanda, legge risposta
            for (Domanda d : domandeRandom) {
                out.println("DOMANDA|" + d.testo); //invia la domanda
                String risposta = in.readLine();//legge la risposta
                if (risposta == null) {
                    //  disconnessione inattesa del client durante il quiz.
                    throw new IOException("Client disconnesso durante il quiz."); 
                }
                risposteClient.add(risposta.trim());//aggiunge la risposta, toglie spazio finale
            }

            // 4) calcola punteggio
            int punteggioTotale = 0;
            for (int i = 0; i < domandeRandom.size(); i++) {
                String risposta = risposteClient.get(i);
                if (!risposta.isEmpty() && risposta.equalsIgnoreCase(domandeRandom.get(i).rispostaCorretta)) {//ignora maiuscole e minuscole nel confronto
                    punteggioTotale += domandeRandom.get(i).punteggio;
                }
            }

            //5 aggiorna classifica
            UtenteSessione u = new UtenteSessione(nome, punteggioTotale);
            synchronized (Server.classifica) { // blocca la classifica per l'accesso sicuro da più thread.
                Server.classifica.add(u);
                salvaClassifica("classifica.txt");
            }

            

            // 5) Invia il punteggio finale al client.
            out.println("PUNTEGGIO|" + punteggioTotale);
            
            // 6) Mantenimento della connessione e gestione dei comandi post-quiz
            String line;
            while ((line = in.readLine()) != null) {
                // se riceve chiudi connessione esce dal loop per questo client
                if (line.equals("CHIUDI_CONNESSIONE")) {
                    break;
                }
                
                // manda la classifica in broadcast a tutti ogni volta che un client la richiede poichè ha terminato la gara
                if (line.equals("DAMMI_CLASSIFICA")) {
                     Server.broadcastClassifica(); // invia la classifica a tutti i client attivi.
                }
            }

        } catch (IOException e) {
            System.out.println("Client disconnesso o errore: " + e.getMessage());
        } finally {
            try {
                socket.close(); // chiude la socket di comunicazione.
            } catch (IOException e) {
                e.printStackTrace();
            }
            // rimuove questo gestore
            synchronized (Server.clientHandlers) {
                Server.clientHandlers.remove(this);
            }
        }
    }

    // metodo per salvare la classifica su file in modo sincronizzato.
    private void salvaClassifica(String filename) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
                for (UtenteSessione u : Server.classifica) {
                    pw.println(u.nome + "|" + u.punteggio);
                }
            } catch (IOException e) {
                System.err.println("Errore nel salvataggio classifica: " + e.getMessage());
            }
    }
}
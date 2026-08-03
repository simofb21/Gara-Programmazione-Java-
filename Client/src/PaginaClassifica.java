import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gui che vede la classifica del quiz in una tabella
 * . Gestisce la richiesta della classifica iniziale al server 
 * e l'aggiornamento dinamico della tabella in base ai broadcast ricevuti.
 * * Necessita della classe 'Giocatore' (o equivalente) per strutturare i dati della classifica.
 * @author simof
 */
public class PaginaClassifica extends JFrame {

    private ConnessioneSocket conn;
    private int punteggioPersonale;

    private JTable tabella;
    private DefaultTableModel model;
    private JLabel lblPunteggio;

    /**
     * Costruttore della Pagina Classifica.
     * @param conn L'oggetto ConnessioneSocket per la comunicazione con il server.
     * @param punteggioPersonale Il punteggio ottenuto dal giocatore corrente.
     */
    public PaginaClassifica(ConnessioneSocket conn, int punteggioPersonale) {

        this.conn = conn;
        this.punteggioPersonale = punteggioPersonale;

        //configurazione finestra(qua la creiamo noi , non netbeans)
        setTitle("Classifica Quiz");
        setSize(450, 400);
        setLocationRelativeTo(null); // centra la finestra
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        lblPunteggio = new JLabel("Hai realizzato " + punteggioPersonale + " punti su 50 in totale", SwingConstants.CENTER);
        add(lblPunteggio, "North");//lo mette in alto nella finestra

        // configurazione della JTable per mostrare i dati della classifica.
        model = new DefaultTableModel(new String[]{"Nome", "Punteggio"}, 0);
        tabella = new JTable(model);
        tabella.setEnabled(false); // rende la tabella non modificabile.

        // aggiunge la tabella a uno JScrollPane per gestire lo scorrimento.
        add(new JScrollPane(tabella), "Center");

        setVisible(true); //rende la tabella visibile

        
        conn.inviaRisposta("DAMMI_CLASSIFICA"); //chiede la classifica

        new Thread(this::ascolta).start();//in un thread, prende la classifica attuale e gli aggiornamenti futuri
        

        // listener che si attiva quando l'utente chiude la GUI.
        addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
            try {
                conn.inviaRisposta("CHIUDI_CONNESSIONE"); //dice al server di chiudere la comunicazione 
                conn.chiudi(); // Chiude la socket lato client.
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            }
        });
    }

    
    private void ascolta() { //thread di ascolto che non blocca la gui
        new Thread(() -> {
            try {
                while (true) {
                    String msg = conn.leggiMessaggio();
                    if (msg == null) {//se non riceve niente dal server
                        Thread.sleep(100); // fa una pausa per non saturare la CPU in assenza di dati.
                        continue;
                    }
                    if (msg.startsWith("CLASSIFICA|")) { //se ottiene la classifica aggiorna la tabella
                        aggiornaTabella(msg);
                    }
                }
            } catch (Exception e) {
               //segnalazione connessione persa
                JOptionPane.showMessageDialog(this,
                        "Connessione persa con il server.",
                        "Errore",
                        JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }
    
   //metodo che aggiorna la tabella
    private void aggiornaTabella(String msg) {
        /*
        se nome = mario e punti = 10 , lui ottiene CLASSIFICA|Mario=10;altro=20;
        */
        String dati = msg.substring("CLASSIFICA|".length());//toglie la parte di CLASSIFICA |
        String[] utenti = dati.split(";");//crea un array di stringhe utenti che ha per esempio MARIO=10 in indice 0

        List<Giocatore> lista = new ArrayList<>();

        for (String u : utenti) {
            if (u.isEmpty()) continue;

            String[] parti = u.split("="); //divide utente in 2 parti

            if (parti.length == 2) {
                String nome = parti[0]; //la prima parte è il nome, la seconda il punteggio
                int punti = Integer.parseInt(parti[1]);
                lista.add(new Giocatore(nome, punti));
            }
        }

        // rdina la lista dei giocatori in ordine decrescente di punteggio.
        lista.sort((a, b) -> b.getPunteggio() - a.getPunteggio());

        SwingUtilities.invokeLater(() -> {//aggiornamento gui
            model.setRowCount(0); // rimuove tutte le righe attuali.

            // aggiunge le righe ordinate e aggiornate al modello della tabella.
            for (Giocatore g : lista) {
                double percentuale = (g.getPunteggio() / 50.0) * 100.0;
                model.addRow(new Object[]{g.getNome(),( percentuale + "%")});            }
        });
    }
}
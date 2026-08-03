/*
* Author : Simone Fusar Bassini e Andrea Cornetti
*Classe domanda , contiene, il testo della domanda, il punteggio assegnato se si risponde corretamente e la risposta corretta alla domanda
*( queste informazioni saranno contenute sul fiile)
*/

public class Domanda {
    String testo;
    String rispostaCorretta;
    int punteggio;

    public Domanda(String t, String r, int p) {
        this.testo = t;
        this.rispostaCorretta = r;
        this.punteggio = p;
    }
}

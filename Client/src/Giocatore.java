/**
 *
 * @author simof
 * classe che viene usata per gli utenti della classifica
 */
public class Giocatore {
    private String nome;
    private int punteggio;
    public Giocatore(String n, int p) {
        nome = n;
        punteggio = p;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getPunteggio() {
        return punteggio;
    }

    public void setPunteggio(int punteggio) {
        this.punteggio = punteggio;
    }
}


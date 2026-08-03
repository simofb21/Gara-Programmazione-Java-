/**
 *classe che gestisce i metodi per la connessione via socket
 * @author simof
 */
import java.io.*;
import java.net.*;

public class ConnessioneSocket {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ConnessioneSocket(String ip, int port) throws IOException {
        socket = new Socket(ip, port); //socket che si fa dare ip e porta su cui avviarsi
        out = new PrintWriter(socket.getOutputStream(), true); //invio via socket
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream())); //lettore socket
    }

    public void inviaNome(String nome) {
        out.println(nome);
    }

    public String leggiMessaggio() throws IOException {
        return in.readLine();
    }

    public void inviaRisposta(String risposta) {
        out.println(risposta);
    }

    public void chiudi() throws IOException {
        socket.close();
    }
}

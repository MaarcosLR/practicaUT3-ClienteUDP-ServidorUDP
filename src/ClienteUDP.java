import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClienteUDP {

    private static final Integer PORT = 5555; // Puerto del servidor
    private static final String SERVER_IP = "localhost"; // Dirección IP del servidor
    private static final String filePath = "files/ficheroDePruebaTamanoGrande.txt"; // Ruta del archivo a enviar
    private static final int CHUNK_SIZE = 512; // Tamaño de cada bloque en bytes (512 bytes)

    public static void main(String[] args) throws IOException {
        // Intentamos establecer un socket para comunicación UDP
        try (DatagramSocket ds = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(SERVER_IP);

            // Leer el archivo completo como un arreglo de bytes
            Path path = Paths.get(filePath);
            byte[] fileContent = Files.readAllBytes(path);

            // Calcular el número total de bloques necesarios para enviar el archivo
            int totalChunks = (int) Math.ceil((double) fileContent.length / CHUNK_SIZE);
            System.out.println("Enviando " + totalChunks + " bloques...");

            // Enviar los bloques uno por uno
            for (int i = 0; i < totalChunks; i++) {
                // Calcular el desplazamiento dentro del archivo y la longitud del bloque actual
                int offset = i * CHUNK_SIZE;
                int length = Math.min(CHUNK_SIZE, fileContent.length - offset);

                // Crear un buffer para el bloque (4 bytes adicionales para el número de bloque)
                byte[] chunk = new byte[length + 4];
                System.arraycopy(fileContent, offset, chunk, 4, length);

                // Escribir el número de bloque en los primeros 4 bytes del chunk (sin signo)
                chunk[0] = (byte) ((i >> 24) & 0xFF);
                chunk[1] = (byte) ((i >> 16) & 0xFF);
                chunk[2] = (byte) ((i >> 8) & 0xFF);
                chunk[3] = (byte) (i & 0xFF);

                // Crear un paquete UDP para enviar el bloque
                DatagramPacket dp = new DatagramPacket(chunk, chunk.length, address, PORT);
                ds.send(dp); // Enviar el paquete al servidor

                // Esperar confirmación (ACK) del servidor
                byte[] ackBuffer = new byte[4]; // Buffer para recibir el ACK
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                ds.setSoTimeout(3000); // Establecer un timeout de 3 segundos

                // Bucle para manejar reintentos en caso de que no se reciba el ACK
                boolean ackReceived = false;
                while (!ackReceived) {
                    try {
                        ds.receive(ackPacket); // Recibir el paquete ACK
                        // Convertir el ACK recibido en un entero sin signo
                        int ackChunkNumber = ((ackBuffer[0] & 0xFF) << 24) | ((ackBuffer[1] & 0xFF) << 16) |
                                ((ackBuffer[2] & 0xFF) << 8) | (ackBuffer[3] & 0xFF);
                        if (ackChunkNumber == i) { // Verificar que el ACK corresponde al bloque enviado
                            System.out.println("Confirmación recibida para el bloque: " + i);
                            ackReceived = true;
                        }
                    } catch (SocketTimeoutException e) {
                        // En caso de timeout, reenviar el bloque
                        System.out.println("Timeout, reenviando bloque: " + i);
                        ds.send(dp);
                    }
                }
            }

            // Enviar un mensaje especial para indicar que la transferencia ha terminado
            byte[] finMessage = "FIN".getBytes();
            DatagramPacket finPacket = new DatagramPacket(finMessage, finMessage.length, address, PORT);
            ds.send(finPacket);
            System.out.println("Transferencia completada.");

        } catch (IOException e) {
            throw new IOException(e);
        }

    }

}

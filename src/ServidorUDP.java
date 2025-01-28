import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeMap;

public class ServidorUDP {

    private static final Integer PORT = 5555; // Puerto en el que escucha el servidor
    private static final String OUTPUT_PATH = "result/archivo_recibido.txt"; // Ruta donde se guardará el archivo recibido
    private static final int CHUNK_SIZE = 512; // Tamaño de cada bloque en bytes

    public static void main(String[] args) throws IOException {
        // Establecer un socket UDP para recibir datos
        try (DatagramSocket ds = new DatagramSocket(PORT)) {
            System.out.println("Servidor UDP escuchando el puerto: " + PORT);

            TreeMap<Integer, byte[]> receivedChunks = new TreeMap<>(); // Estructura ordenada para almacenar bloques
            boolean transferComplete = false;

            // Bucle principal para recibir los bloques del cliente
            while (!transferComplete) {
                byte[] buffer = new byte[CHUNK_SIZE + 4]; // Buffer para recibir el bloque (+4 para el número de bloque)
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                ds.receive(dp); // Recibir un paquete UDP del cliente

                // Verificar si es el mensaje de finalización
                String message = new String(dp.getData(), 0, dp.getLength());
                if (message.equals("FIN")) {
                    transferComplete = true; // Finalizar la transferencia
                    System.out.println("Transferencia completada.");
                    break;
                }

                // Extraer el número de bloque del paquete (sin signo)
                int chunkNumber = ((buffer[0] & 0xFF) << 24) | ((buffer[1] & 0xFF) << 16) |
                        ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
                System.out.println("Bloque recibido: " + chunkNumber);

                // Extraer los datos del bloque y almacenarlos en el mapa
                byte[] chunkData = new byte[dp.getLength() - 4];
                System.arraycopy(buffer, 4, chunkData, 0, dp.getLength() - 4);
                receivedChunks.put(chunkNumber, chunkData);

                // Enviar confirmación (ACK) al cliente
                byte[] ackBuffer = new byte[4];
                ackBuffer[0] = (byte) ((chunkNumber >> 24) & 0xFF);
                ackBuffer[1] = (byte) ((chunkNumber >> 16) & 0xFF);
                ackBuffer[2] = (byte) ((chunkNumber >> 8) & 0xFF);
                ackBuffer[3] = (byte) (chunkNumber & 0xFF);
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, dp.getAddress(), dp.getPort());
                ds.send(ackPacket);
            }

            // Reconstruir el archivo completo a partir de los bloques recibidos
            byte[] fileContent = new byte[receivedChunks.size() * CHUNK_SIZE];
            int offset = 0;
            for (byte[] chunk : receivedChunks.values()) {
                System.arraycopy(chunk, 0, fileContent, offset, chunk.length);
                offset += chunk.length;
            }

            // Guardar el archivo reconstruido en el disco
            Path outputPath = Paths.get(OUTPUT_PATH);
            Files.createDirectories(outputPath.getParent()); // Crear directorios si no existen
            Files.write(outputPath, fileContent);
            System.out.println("Archivo guardado en: " + OUTPUT_PATH);

        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}

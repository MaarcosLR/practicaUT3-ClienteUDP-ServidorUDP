# UT3 Práctica

## **Explicación general del código ServidorUDP y ClienteUDP**

### **¿Qué hace el programa?**

El programa permite enviar un archivo desde un cliente a un servidor en fragmentos pequeños llamados *"chunks"* o bloques. Cada fragmento se envía mediante paquetes UDP, y el servidor confirma que ha recibido cada uno correctamente. Cuando todos los fragmentos han llegado, el servidor los junta para reconstruir el archivo original y lo guarda en una ruta determinada la cual se puede personalizar.

El **ServidorUDP**, está escuchando el puerto `5555`. De ahí recibe los *"chunks"* o bloques en orden porque cada chunk tiene un número. Cuando esto ocurre, manda un mensaje de confirmación (básicamente esto se denomina como **ACK**) al cliente. Luego, cuando ha recibido todos los chunks, los reconstruye y los guarda en una ruta determinada que se puede personalizar.

El **ClienteUDP** lee el archivo que se ha puesto en una ruta personalizable y lo divide en *"chunks"* de `512 bytes`. Lo envía junto a su número de bloque correspondiente para que el servidor reconozca el orden con el **TreeMap**. Espera a que el servidor envíe un mensaje de confirmación (**ACK**) para poder enviar el siguiente chunk. Cuando ha terminado de enviar todos, envía un mensaje especial (no visible por el usuario) para indicar que ya ha terminado y que el servidor no espere indefinidamente.

## **Cuestión 1: ¿Qué ventajas tiene UDP frente a TCP para esta aplicación?**

UDP es más rápido que TCP, ya que UDP no necesita establecer una conexión o confirmar que los datos hayan llegado correctamente. Este tipo de protocolo se utiliza sobre todo en videojuegos, donde la velocidad de transmisión de datos es lo más importante.

## **Cuestión 2: ¿Cómo garantizarías la integridad de los datos transmitidos?**

Se podría garantizar con las siguientes medidas:

- Utilizar un número de identificación para cada chunk de datos, permitiendo al servidor reconstruir correctamente el archivo.
- Confirmar la recepción de cada bloque mediante **ACK**.
- Si el servidor no recibe un bloque, debe solicitarlo nuevamente al cliente mediante un **timeout**, para que este lo vuelva a enviar.

## **Cuestión 3: ¿Qué impacto podría tener el tamaño de los paquetes en el rendimiento de la transferencia?**

El tamaño está configurado en `512 bytes`, que es un valor razonable. Consideraciones:

- Si el archivo es demasiado grande, tardará más, generando mayor latencia y aumentando la probabilidad de pérdida de paquetes.
- Si es un archivo vacío, el cliente no podrá enviar ningún paquete al servidor porque no habrá datos. Si primero se ejecuta el cliente y luego el servidor, el servidor no recibirá nada.
- Si se inicia primero el servidor y luego el cliente, como el cliente no ha enviado ningún bloque, el servidor simplemente reconstruirá un archivo vacío.

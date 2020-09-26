/*
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * PortEnumerator.java
 *
 * Created on 21 maj 2004, 09:55
 */
package name.prokop.bart.fps.util;

import gnu.io.*;
import name.prokop.bart.fps.FiscalPrinterException;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Klasa pomocnicza do obsługi biblioteki rxtx.<br>
 * Szczegółowe informacje o tej bibliotece znajdują się na
 * http://rxtx.qbang.org/wiki. Biblioteka pochodzi z rxtx.org
 * To, czy porty szeregowe są prawidłowo rozpoznawalne w naszym systemie można
 * stwierdzić uruchamiając następujące klasy:<br>
 * <CODE>java -cp "RXTXcomm.jar;bart.jar" name.prokop.bart.hardware.comm.PortEnumerator</CODE>
 *
 * Wynik powinien być mniej więcej taki:<br>
 * <CODE>Stable Library<br>
 * =========================================<br>
 * Native lib Version = RXTX-2.1-7<br>
 * Java lib Version = RXTX-2.1-7<br>
 * 2 port(ow) szeregowych: COM1 COM2<br>
 * 1 port(ow) rownoleglych: LPT1<br></CODE>
 *
 * Alternatywnie można użyć komendy:
 * <CODE>javaw -cp "RXTXcomm.jar;bart.jar" name.prokop.bart.hardware.comm.AvaiablePortsDialog</CODE>
 *
 * @author Bartłomiej Prokop
 */
public class PortEnumerator {

    /**
     * Drukuje na konsoli listę portów szeregowych, jakie biblioteka rxtx
     * znalazła w komputerze.
     *
     * @param args Bez znaczenia
     */
    public static void main(String[] args) {
        String[] portList;

        portList = getPortList();
        System.out.print(portList.length);
        System.out.print(" port(ow):");
        for (int i = 0; i < portList.length; i++) {
            System.out.print(portList[i] + " ");
        }
        System.out.println();

        System.out.println("*************************************************");

        portList = getSerialPortList();
        System.out.print(portList.length);
        System.out.println(" port(ow) szeregowych:");
        for (int i = 0; i < portList.length; i++) {
            System.out.print(portList[i] + " otwieram ");
            try {
                CommPort p = getSerialPort(portList[i]);
                p.close();
                System.out.print("OK");
            } catch (IOException e) {
                System.out.print("nie udało się: " + e.getMessage());
            }
            System.out.println();
        }
        System.out.println();

        portList = getParallelPortList();
        System.out.print(portList.length);
        System.out.println(" port(ow) rownoleglych:");
        for (int i = 0; i < portList.length; i++) {
            System.out.print(" " + portList[i]);
        }
        System.out.println();
    }

    public static String[] getPortList() {
        Enumeration portList;
        portList = CommPortIdentifier.getPortIdentifiers();

        int portCount = 0;
        while (portList.hasMoreElements()) {
            portList.nextElement();
            portCount++;
        }

        String[] retVal = new String[portCount];

        portCount = 0;
        portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            retVal[portCount++] = portId.getName();
        }

        return retVal;
    }

    /**
     * Zwraca liste portow szeregowych
     *
     * @return Zwraca liste portow szeregowych. Zwracana jest tablica stringów.
     * Stringi te można użyć w funkcji getSerialPort
     */
    public static String[] getSerialPortList() {
        Enumeration portList;
        portList = CommPortIdentifier.getPortIdentifiers();

        int serialPortCount = 0;
        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                serialPortCount++;
            }
        }

        String[] retVal = new String[serialPortCount];

        serialPortCount = 0;
        portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                retVal[serialPortCount++] = portId.getName();
            }
        }

        return retVal;
    }

    /**
     * Zwraca liste portow rownoleglych
     *
     * @return Zwraca liste portow rownoleglych
     */
    public static String[] getParallelPortList() {
        Enumeration portList;
        portList = CommPortIdentifier.getPortIdentifiers();

        int serialPortCount = 0;
        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_PARALLEL) {
                serialPortCount++;
            }
        }

        String[] retVal = new String[serialPortCount];

        serialPortCount = 0;
        portList = CommPortIdentifier.getPortIdentifiers();
        while (portList.hasMoreElements()) {
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            if (portId.getPortType() == CommPortIdentifier.PORT_PARALLEL) {
                retVal[serialPortCount++] = portId.getName();
            }
        }

        return retVal;
    }

    /**
     * Zwraca <b>otwarty</b> port szeregowy o zadanej nazwie
     *
     * @return Zwraca port szeregowy o zadanej nazwie
     * @param portName Nazwa portu
     * @throws IOException when unable to enumerate serial ports
     */
    public static SerialPort getSerialPort(String portName) throws IOException {
        try {
            CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(portName);
            //Enumeration portList = CommPortIdentifier.getPortIdentifiers();
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                if (portId.getName().equals(portName)) {
                    return (SerialPort) portId.open("Bart Prokop Comm Helper", 3000);
                }
            }
        } catch (NoSuchPortException e) {
            throw new IOException("NoSuchPortException @ " + portName, e);
        } catch (PortInUseException e) {
            throw new IOException("PortInUseException @ " + portName, e);
        }

        throw new IOException("To nie jest port szeregowy");
    }

}

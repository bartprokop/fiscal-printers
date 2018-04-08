/*
 * Copyright 2018 Bartłomiej P. Prokop
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.prokop.bart.fps;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import name.prokop.bart.fps.datamodel.SaleLine;
import name.prokop.bart.fps.datamodel.Slip;
import name.prokop.bart.fps.datamodel.SlipPayment;
import name.prokop.bart.fps.datamodel.VATRate;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Bartłomiej Prokop
 */
public class CloudPrint {

    public static void main(String... args) throws Exception {
        args = new String[]{"Console", "COM1", "898288f0-bf79-4827-9d11-6b0b492e354c"};
        if (args.length != 3) {
            printHelp();
            return;
        }

        final FiscalPrinter.Type type = FiscalPrinter.Type.valueOf(args[0]);
        final String comPort = args[1];
        final String printerId = args[2];

        System.out.println("Printer: " + type + ", port: " + comPort + ", id: " + printerId + ".");
        final URL url = new URL("https://fiscal-printer.appspot.com/v1/queue/" + printerId);
        System.out.println("Server URL: " + url);

        final char[] X = new char[]{'-', '\\', '|', '/'};
        int counter = 0;
        while (true) {
            System.out.print(X[counter++ % X.length]);
            Thread.sleep(500);
            try {
                Slip slip = retrieveSlip(url);
                System.out.print('\b');
                if (slip != null) {
                    FiscalPrinter fiscalPrinter = type.getFiscalPrinter(comPort);
                    fiscalPrinter.print(slip);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e);
            }
        }
    }

    private static Slip retrieveSlip(URL url) throws Exception {
        final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        if (conn.getResponseCode() == 404) {
            return null;
        }

        InputStream is = conn.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        int ch;
        StringBuilder sb = new StringBuilder();
        while ((ch = isr.read()) != -1) {
            sb.append((char) ch);
        }

        return parseSlip(sb.toString());
    }

    private static void printHelp() {
        System.out.println("Usage: PrinterType COMx printer-qeueue-uuid");
        System.out.println("Avaiable PrinterTypes:");
        for (FiscalPrinter.Type t : FiscalPrinter.Type.values()) {
            System.out.print(" " + t.name());
        }
    }

    private static Slip parseSlip(String string) {
        final JSONObject jsonReceipt = new JSONObject(string);

        Slip slip = new Slip();
        slip.setReference(jsonReceipt.optString("reference", null));
        slip.setCashierName(jsonReceipt.optString("cashier", null));
        slip.setCashbox(jsonReceipt.optString("register", null));

        final JSONArray jsonItems = jsonReceipt.optJSONArray("items");
        for (int i = 0; i < jsonItems.length(); i++) {
            final JSONObject jsonItem = jsonItems.getJSONObject(i);
            SaleLine sl = new SaleLine();
            sl.setName(jsonItem.optString("description", null));
            sl.setAmount(jsonItem.optDouble("amount", 1.0));
            sl.setPrice(jsonItem.optDouble("unitPrice", 0.01));
            sl.setTaxRate(VATRate.valueOf(jsonItem.optString("vatRate", "VAT23")));
            slip.addLine(sl);
        }

        slip.addPayment(SlipPayment.PaymentType.Cash, slip.getTotal(), null);

        return slip;
    }

}

/*
 * Copyright 2018 proko.
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
package name.prokop.bart.fps.drivers;

import name.prokop.bart.fps.FiscalPrinter;
import name.prokop.bart.fps.FiscalPrinterException;
import name.prokop.bart.fps.datamodel.Invoice;
import name.prokop.bart.fps.datamodel.Slip;
import name.prokop.bart.fps.datamodel.SlipExamples;

/**
 *
 * @author Bart Prokop
 */
public class ConsoleDump implements FiscalPrinter {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ConsoleDump fp;

        if (args.length != 0) {
            fp = (ConsoleDump) ConsoleDump.getFiscalPrinter(args[0]);
        } else {
            fp = (ConsoleDump) ConsoleDump.getFiscalPrinter("COM1");
        }

        try {
            fp.print(SlipExamples.getOneCentSlip());
            //fp.printTest();
            //fp.printDailyReport();
        } catch (FiscalPrinterException e) {
            e.printStackTrace(System.err);
        }
    }

    public static FiscalPrinter getFiscalPrinter(String comPortName) {
        return new ConsoleDump(comPortName);
    }

    private ConsoleDump(String portName) {
        System.out.println("ConsoleDump(" + portName + ")");
    }

    @Override
    public void print(Slip slip) throws FiscalPrinterException {
        slip = SlipExamples.demo(slip);
        System.out.println("print(Slip" + slip + ")");
    }

    @Override
    public void print(Invoice invoice) throws FiscalPrinterException {
        System.out.println("print(" + invoice + ")");
    }

    @Override
    public void openDrawer() throws FiscalPrinterException {
        System.out.println("openDrawer()");
    }

    @Override
    public void printDailyReport() throws FiscalPrinterException {
        System.out.println("printDailyReport()");
    }

}

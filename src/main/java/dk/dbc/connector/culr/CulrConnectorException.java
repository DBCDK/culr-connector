/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.connector.culr;

public class CulrConnectorException extends Exception {
    public CulrConnectorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

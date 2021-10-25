/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact:
 *
 * Authors:
 */

package main.pojo;

import main.proxy.ISentence;
import main.proxy.JvnProxy;
import main.proxy.Transactional;

import java.io.Serializable;

public class Sentence implements Serializable, ISentence, Transactional {
	private static final long serialVersionUID = 1L;
	private String 	data;
	private boolean transactionRunning;

	public Sentence() {
		data = "";
	}

	public static ISentence newSharedInstance(String jon) {
		try {
			return (ISentence) JvnProxy.newInstance(jon);
		} catch (JvnException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void write(String text) {
		data = text;
	}

	@Override
	public String read() {
		return data;	
	}

	@Override
	public void startTransaction() {
		transactionRunning = true;
	}

	@Override
	public void endTransaction() {
		transactionRunning = false;
	}

	@Override
	public boolean isTransactionRunning() {
		return transactionRunning;
	}
}
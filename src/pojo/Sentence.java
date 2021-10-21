/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact:
 *
 * Authors:
 */

package pojo;

import proxy.ISentence;
import proxy.JvnProxy;

import java.io.Serializable;

public class Sentence implements Serializable, ISentence {
	private static final long serialVersionUID = 1L;
	private String 	data;

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
}
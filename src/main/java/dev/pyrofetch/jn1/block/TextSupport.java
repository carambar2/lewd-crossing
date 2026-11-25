package dev.pyrofetch.jn1.block;

import java.util.List;

/**
 * Ce qui porte du texte qu'on écrit à la main : une note, un écran mural.
 *
 * <p>Les deux partagent l'éditeur ({@code NoteScreen}), le paquet
 * d'enregistrement et ses bornes ({@link dev.pyrofetch.jn1.network.SaveNotePayload}),
 * et les contrôles du serveur ({@link NoteHandler}) — portée, verrou.
 */
public interface TextSupport {
	List<String> lignes();

	boolean verrouillée();

	void setLignes(List<String> lignes, boolean verrou);
}

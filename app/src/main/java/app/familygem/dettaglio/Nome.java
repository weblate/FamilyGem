package app.familygem.dettaglio;

import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import app.familygem.Dettaglio;
import app.familygem.Globale;
import app.familygem.Memoria;
import app.familygem.R;
import app.familygem.U;
import static app.familygem.Globale.gc;

public class Nome extends Dettaglio {

	Name n;

	@Override
	public void impagina() {
		setTitle( R.string.name );
		mettiBava( "NAME", null );
		n = (Name) casta( Name.class );
		if( Globale.preferenze.esperto )
			metti( getString(R.string.value), "Value" );
		else {
			String epiteto = n.getValue();
			String nome = epiteto.replaceAll( "/.*?/", "" ).trim();
			creaPezzo( getString(R.string.name), nome, 4043, false );
			String cognome = "";
			if( epiteto.indexOf('/') < epiteto.lastIndexOf('/') )
				cognome = epiteto.substring( epiteto.indexOf('/') + 1, epiteto.lastIndexOf('/') ).trim();
			creaPezzo( getString(R.string.surname), cognome, 6064, false );
		}
		metti( getString(R.string.type), "Type", false, false );  // _type non Gedcom standard
		metti( getString(R.string.prefix), "Prefix" );
		metti( getString(R.string.given), "Given", Globale.preferenze.esperto, false );
		metti( getString(R.string.nickname), "Nickname" );
		metti( getString(R.string.surname_prefix), "SurnamePrefix" );
		metti( getString(R.string.surname), "Surname", Globale.preferenze.esperto, false );
		metti( getString(R.string.suffix), "Suffix" );
		metti( getString(R.string.married_name), "MarriedName", false, false ); // _marrnm
		metti( getString(R.string.aka), "Aka", false, false );	// _aka
		metti( getString(R.string.romanized), "Romn", false, false );
		metti( getString(R.string.phonetic), "Fone", false, false );
		mettiEstensioni( n );
		U.mettiNote( box, n, true );
		U.mettiMedia( box, n, true );	// Mi sembra strano che un Name abbia Media.. comunque..
		U.citaFonti( box, n );
	}

	@Override
	public void elimina() {
		Person costui = gc.getPerson( Globale.individuo );
		costui.getNames().remove( n );
		U.aggiornaDate( costui );
		Memoria.annullaIstanze( n );
	}
}

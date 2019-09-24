// Lista dei Submitter (autori)

package app.familygem;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.Submitter;
import java.util.List;
import app.familygem.dettaglio.Autore;
import static app.familygem.Globale.gc;

public class Podio extends Fragment {

	@Override
	public void onCreate( Bundle bandolo ) {
		super.onCreate( bandolo );
	}

	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle stato ) {
		List<Submitter> listAutori = gc.getSubmitters();
		((AppCompatActivity) getActivity()).getSupportActionBar().setTitle( listAutori.size() + " " + getString(R.string.submitters).toLowerCase() );
		setHasOptionsMenu(true);
		View vista = inflater.inflate( R.layout.magazzino, container, false);
		LinearLayout scatola = vista.findViewById( R.id.magazzino_scatola );
		for( final Submitter autor : listAutori ) {
			View vistaPezzo = inflater.inflate( R.layout.magazzino_pezzo, scatola, false );
			scatola.addView( vistaPezzo );
			String nome = autor.getName();
			if( nome == null || nome.isEmpty() )
				nome = getString( android.R.string.unknownName );
			((TextView)vistaPezzo.findViewById( R.id.magazzino_nome )).setText( nome );
			vistaPezzo.findViewById( R.id.magazzino_archivi ).setVisibility( View.GONE );
			vistaPezzo.setOnClickListener( new View.OnClickListener() {
				public void onClick( View vista ) {
					/* non usato
					if( getActivity().getIntent().getBooleanExtra("podioScegliAutore",false) ) {
						Intent intento = new Intent();
						intento.putExtra("idAutore", autor.getId() );
						getActivity().setResult( Activity.RESULT_OK, intento );
						getActivity().finish();
					} else{*/
					Memoria.setPrimo( autor );
					startActivity( new Intent( getContext(), Autore.class ) );
				}
			});
			registerForContextMenu( vistaPezzo );
			vistaPezzo.setTag( autor );
		}
		return vista;
	}

	// Elimina un autore
	// Todo mi sa che andrebbe cercato eventuale SubmitterRef in tutti i record
	public static void eliminaAutore( Submitter aut ) {
		Header testa = gc.getHeader();
		if( testa != null && testa.getSubmitterRef() != null
				&& testa.getSubmitterRef().equals(aut.getId()) ) {
			testa.setSubmitterRef( null );
		}
		gc.getSubmitters().remove( aut );
		if( gc.getSubmitters().isEmpty() )
			gc.setSubmitters( null );
	}

	// Crea un Autore nuovo, se riceve un contesto lo apre in modalità editore
	public static Submitter nuovoAutore( Context contesto ) {
		Submitter subm = new Submitter();
		subm.setId( U.nuovoId(gc,Submitter.class) );
		subm.setName( "" );
		U.aggiornaDate( subm );
		gc.addSubmitter( subm );
		if( contesto != null ) {
			Memoria.setPrimo( subm );
			contesto.startActivity( new Intent( contesto, Autore.class ) );
		}
		return subm;
	}

	// menu opzioni nella toolbar
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater ) {
		menu.add( 0, 0, 0, R.string.new_m );
	}
	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId() ) {
			case 0:
				nuovoAutore( getContext() );
				U.salvaJson( true );
				return true;
			default:
				return false;
		}
	}

	// Menu contestuale
	View vistaAutore;
	@Override
	public void onCreateContextMenu(ContextMenu menu, View vista, ContextMenu.ContextMenuInfo info ) {
		vistaAutore = vista;
		if( gc.getHeader() == null || gc.getHeader().getSubmitter(gc) == null || !gc.getHeader().getSubmitter(gc).equals( vista.getTag() ) )
			menu.add( 0, 0, 0, R.string.make_default );
		if( !U.autoreHaCondiviso((Submitter)vista.getTag()) ) // può essere eliminato solo se non ha mai condiviso
			menu.add( 0, 1, 0, R.string.delete );
		// todo spiegare perché non può essere eliminato?
	}
	@Override
	public boolean onContextItemSelected( MenuItem item ) {
		Submitter subm = (Submitter) vistaAutore.getTag();
		switch( item.getItemId() ) {
			case 0:
				Header testa = gc.getHeader();
				if( testa == null ) {
					testa = AlberoNuovo.creaTestata( Globale.preferenze.idAprendo + ".json" );
					gc.setHeader( testa );
				}
				testa.setSubmitterRef( subm.getId() );
				U.salvaJson( false, subm );
				return true;
			case 1:
				// Todo conferma elimina
				eliminaAutore( subm );
				vistaAutore.setVisibility( View.GONE );
				U.salvaJson(false);
				return true;
		}
		return false;
	}
}
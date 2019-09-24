package app.familygem;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.CharacterSet;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomVersion;
import org.folg.gedcom.model.Generator;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;

public class AlberoNuovo extends AppCompatActivity {

	@Override
	protected void onCreate( Bundle bandolo ) {
		super.onCreate( bandolo );
		setContentView(R.layout.albero_nuovo);

		// Alla prima apertura nasconde la freccia indietro
		if( Globale.preferenze.idAprendo == 0 )
			getSupportActionBar().setDisplayHomeAsUpEnabled( false );

		// Parte con un albero vuoto
		findViewById( R.id.bottone_albero_vuoto ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( View vista ) {
				View vistaMessaggio = LayoutInflater.from( vista.getContext() ).inflate(R.layout.albero_nomina, null);
				AlertDialog.Builder builder = new AlertDialog.Builder( vista.getContext() );
				builder.setView( vistaMessaggio ).setTitle( R.string.title );
				TextView vistaTesto = vistaMessaggio.findViewById( R.id.nuovo_nome_testo );
				vistaTesto.setText( R.string.modify_later );
				vistaTesto.setVisibility( View.VISIBLE );
				final EditText nuovoNome = vistaMessaggio.findViewById( R.id.nuovo_nome_albero );
				builder.setPositiveButton( R.string.create, new DialogInterface.OnClickListener() {
					public void onClick( DialogInterface dialog, int id ) {
						int num = Globale.preferenze.max() + 1;
						File fileJson = new File( getFilesDir(), num + ".json" );
						Globale.gc = new Gedcom();
						Globale.gc.setHeader( creaTestata( fileJson.getName() ) ); //.getAbsolutePath()
						Globale.gc.createIndexes();
						JsonParser jp = new JsonParser();
						try {
							FileUtils.writeStringToFile( fileJson, jp.toJson(Globale.gc), "UTF-8" );
						} catch (IOException e) {
							Toast.makeText( AlberoNuovo.this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
							e.printStackTrace();
						}
						Globale.preferenze.aggiungi( new Armadio.Cassetto(
								num, nuovoNome.getText().toString(), null, 0, 0, null, null, 0, null ));
						Globale.preferenze.idAprendo = num;
						Globale.preferenze.salva();
						startActivity( new Intent( AlberoNuovo.this, Principe.class ) );
					}
				}).setNeutralButton( R.string.cancel, null );
				AlertDialog dialog = builder.create();
				dialog.show();
				dialog.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE );
			}
		});

		findViewById(R.id.bottone_scarica_esempio).setOnClickListener( new View.OnClickListener() {
			public void onClick( View v ) {
				int perm = ContextCompat.checkSelfPermission(v.getContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if( perm == PackageManager.PERMISSION_DENIED )
					ActivityCompat.requestPermissions( (AppCompatActivity)v.getContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5641 );
				else if( perm == PackageManager.PERMISSION_GRANTED )
					scaricaEsempio();
			}
		});

		findViewById(R.id.bottone_importa_gedcom).setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
				intent.setType( "application/*" );
				startActivityForResult( intent,630 );
			}
		});

		findViewById(R.id.bottone_recupera_backup).setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
				intent.setType( "application/zip" );
				startActivityForResult( intent, 219 );
			}
		});
	}

	// Elabora la risposta alle richieste di permesso
	@Override
	public void onRequestPermissionsResult( int codice, String[] permessi, int[] accordi ) { // If request is cancelled, the result arrays are empty
		if( accordi.length > 0 && accordi[0] == PackageManager.PERMISSION_GRANTED ) {
			if( codice == 5641 ) {
				scaricaEsempio();
			}
		}
	}

	// Apre il diagramma oppure l'elenco degli alberi
	static void caricaAlbero( Context contesto, int idAlbero ) {
		if( Globale.preferenze.caricaAlbero && Alberi.apriGedcom(idAlbero,true) ) {
			// Apre il nuovo albero in diagramma
			contesto.startActivity( new Intent( contesto, Principe.class ) );
		} else
			contesto.startActivity( new Intent( contesto, Alberi.class ) );
	}

	// Scarica da internet un file zip nella cartella Download
	void scaricaEsempio() {
		DownloadManager gestoreScarico = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		// Evita download multipli
		Cursor curso = gestoreScarico.query( new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING) );
		if( curso.moveToFirst() ) {
			curso.close();
			findViewById(R.id.bottone_scarica_esempio).setEnabled(false);
			return;
		}
		String url = "https://drive.google.com/uc?export=download&id=1FT-60avkxrHv6G62pxXs9S6Liv5WkkKf";
		final String percorsoZip = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				+ "/the_Simpsons.zip";
		DownloadManager.Request richiesta = new DownloadManager.Request( Uri.parse( url ) )
				.setTitle( getString(R.string.simpsons_tree) )
				.setDescription( getString(R.string.downloading) )
				.setNotificationVisibility( DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
				.setVisibleInDownloadsUi(false)
				.setDestinationUri( Uri.parse( "file://" + percorsoZip ) );
		gestoreScarico.enqueue( richiesta );
		BroadcastReceiver alCompletamento = new BroadcastReceiver() {
			@Override
			public void onReceive( Context contesto, Intent intento ) {
				decomprimiZip( contesto, percorsoZip, null );
				unregisterReceiver( this );
			}
		};
		registerReceiver( alCompletamento, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE) );
		// ACTION_DOWNLOAD_COMPLETE intende il completamento di QUALSIASI download che è in corso, non solo questo.
	}

	// Decomprime il file zip nella memoria esterna e cartella col numero dell'albero
	// Usato ugualmente da: esempio Simpson, file di backup e alberi condivisi
	static void decomprimiZip( final Context contesto, String percorsoZip, Uri uriZip ) {
		int numAlbero = Globale.preferenze.max() + 1;
		String percorsoImmagini = contesto.getExternalFilesDir(null) + "/" + numAlbero;
		File dirImmagini = new File( percorsoImmagini );
		if( !dirImmagini.exists() )
			dirImmagini.mkdir();
		try {
			InputStream is;
			if( percorsoZip != null )
				is = new FileInputStream( percorsoZip );
			else
				is = contesto.getContentResolver().openInputStream( uriZip );
			ZipInputStream zis = new ZipInputStream( is );
			ZipEntry zipEntry;
			int len;
			byte[] buffer = new byte[1024];
			while( (zipEntry = zis.getNextEntry()) != null ) {
				String percorsoFile;
				if( zipEntry.getName().equals("tree.json") )
					percorsoFile = contesto.getFilesDir() +"/"+ numAlbero + ".json";
				else if( zipEntry.getName().equals("settings.json") )
					percorsoFile = contesto.getCacheDir() +"/settings.json";
				else // è un file nella cartella 'media'
					percorsoFile = percorsoImmagini +"/"+ zipEntry.getName().replace( "media/", "" );
				File newFile = new File( percorsoFile );
				FileOutputStream fos = new FileOutputStream( newFile );
				while( ( len = zis.read( buffer ) ) > 0 ) {
					fos.write( buffer, 0, len );
				}
				fos.close();
			}
			zis.closeEntry();
			zis.close();
			// Legge le impostazioni e le salva nelle preferenze
			File fileImpostazioni = new File( contesto.getCacheDir(),"settings.json" );
			String json = FileUtils.readFileToString( fileImpostazioni, "UTF-8" );
			Gson gson = new Gson();
			Armadio.CassettoCondiviso cassa = gson.fromJson( json, Armadio.CassettoCondiviso.class );
			Armadio.Cassetto cassetto = new Armadio.Cassetto( numAlbero, cassa.titolo, percorsoImmagini,
					cassa.individui, cassa.generazioni, cassa.radice, cassa.condivisioni, cassa.grado, null );
			Globale.preferenze.aggiungi( cassetto );
			fileImpostazioni.delete();
			//fileZip.delete();
			// Albero proveniente da condivisione destinato al confronto
			if( cassa.grado == 9 && confronta(contesto,cassetto) ) {
				cassetto.grado = 20; // lo marchia come derivato
			} else { // Albero di esempio, di backup, o di condivisione
				caricaAlbero( contesto, numAlbero );
			}
			Globale.preferenze.salva();
			U.tosta( (Activity)contesto, R.string.tree_imported_ok );
		} catch( Exception e ) {
			U.tosta( (Activity)contesto, e.getLocalizedMessage() );
		}
	}

	// Importa un file Gedcom scelto col file manager
	// ToDo: aspetta un attimo, non sarebbe meglio usare Gedcom2Json ?
	@Override
	protected void onActivityResult( int requestCode, int resultCode, final Intent data ) {
		super.onActivityResult( requestCode, resultCode, data );
		if( resultCode == RESULT_OK && requestCode == 630 ){
			try {
				Uri uri = data.getData();
				String percorso = U.uriPercorsoFile( uri );	// in Google drive trova solo il nome del file
				File fileGedcom;
				String nomeAlbero;
				String percorsoCartella = null;
				int nuovoNum = Globale.preferenze.max() + 1;
				if( percorso.lastIndexOf('/') > 0 ) {	// se è un percorso completo del file gedcom
					// Apre direttamente il file ged
					fileGedcom = new File( percorso );
					// Percorso della cartella da cui ha caricato il gedcom
					percorsoCartella = fileGedcom.getParent();
					nomeAlbero = fileGedcom.getName();
				}
				else {	// è solo il nome del file 'famiglia.ged'
					// Copia il contenuto del Gedcom in un file temporaneo
					InputStream input = getContentResolver().openInputStream(uri);
					fileGedcom = new File( getCacheDir(), "temp.ged" );
					FileUtils.copyInputStreamToFile( input, fileGedcom );
					if( percorso == null ) nomeAlbero = getString( R.string.tree ) + " " + nuovoNum;
					else nomeAlbero = percorso;
				}
				// Crea l'oggetto Gedcom
				ModelParser mp = new ModelParser();
				Gedcom gc = mp.parseGedcom( fileGedcom );
				if( gc.getHeader() == null ) {
					Toast.makeText( this, R.string.invalid_gedcom, Toast.LENGTH_LONG ).show();
					return;
				}
				gc.createIndexes();  // todo ma dai qui non è necessario
				// Salva il file Json
				if( nomeAlbero.lastIndexOf('.') > 0 )
					nomeAlbero = nomeAlbero.substring(0, nomeAlbero.lastIndexOf('.'));
				PrintWriter pw = new PrintWriter( getFilesDir() + "/" + nuovoNum + ".json" );
				JsonParser jp = new JsonParser();
				pw.print( jp.toJson(gc) );
				pw.close();
				// Salva le impostazioni in preferenze
				String idRadice = U.trovaRadice(gc);
				Globale.preferenze.aggiungi( new Armadio.Cassetto( nuovoNum, nomeAlbero, percorsoCartella,
						gc.getPeople().size(), InfoAlbero.quanteGenerazioni(gc,idRadice), idRadice, null, 0, null ) );
				// Se necessario propone di mostrare le funzioni avanzate TODO il dialogo va pensato meglio quando farlo comparire
				if( !gc.getSources().isEmpty() && !Globale.preferenze.esperto ) {
					/*AlertDialog.Builder dialog = new AlertDialog.Builder( this );
					dialog.setMessage( "L'albero che hai importato sembra piuttosto complesso.\nPer gestirlo vuoi mostrare le funzioni avanzate di Family Ged?" );
					dialog.setPositiveButton( android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialogo, int i ) {
							dialogo.cancel();
							Globale.preferenze.esperto = true;
							Globale.preferenze.salva();
						}
					});
					dialog.setNegativeButton( android.R.string.no, new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialogo, int i ) {
							dialogo.cancel();
						}
					}).show();*/
					Globale.preferenze.esperto = true;
					Globale.preferenze.salva();
				}
				caricaAlbero( this, nuovoNum );
			} catch( Exception e ) {	//IOException | SAXParseException | URISyntaxException |FileNotFoundException |
				Toast.makeText( AlberoNuovo.this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
			}
		}

		// Estrae un file ZIP di backup, mantenendo la compatibilità con il precedente tipo di backup:
		// - un backup di tipo vecchio SOSTITUISCE tutti gli alberi esistenti
		// - un backup nuovo AGGIUNGE un singolo albero
		if( resultCode == RESULT_OK && requestCode == 219 ){
			try {
				boolean ceFileSettings = false;
				boolean ceFilePreferenze = false;
				final ZipInputStream zis = new ZipInputStream( getContentResolver().openInputStream( data.getData() ) );
				ZipEntry zipEntry;
				while( (zipEntry = zis.getNextEntry()) != null ) {
					if(zipEntry.getName().equals( "settings.json" ))
						ceFileSettings = true;
					if(zipEntry.getName().equals( "preferenze.json" ))
						ceFilePreferenze = true;
				}
				zis.closeEntry();
				zis.close();
				if( ceFileSettings ) {
					decomprimiZip( this, null, data.getData() );
				} else if(ceFilePreferenze){
					AlertDialog.Builder builder = new AlertDialog.Builder( AlberoNuovo.this );
					builder.setTitle( R.string.warning );
					builder.setMessage( R.string.old_backup_overwrite );
					builder.setPositiveButton( android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id ) {
							try {
								ZipInputStream zis2 = new ZipInputStream( getContentResolver().openInputStream( data.getData() ) );
								ZipEntry zipEntry2;
								int len;
								byte[] buffer = new byte[1024];
								while( (zipEntry2 = zis2.getNextEntry()) != null ) {
									File newFile = new File( getFilesDir(), zipEntry2.getName() );
									FileOutputStream fos = new FileOutputStream(newFile);
									while ((len = zis2.read(buffer)) > 0) {
										fos.write(buffer, 0, len);
									}
									fos.close();
								}
								zis2.closeEntry();
								zis2.close();
								Globale.avvia( getApplicationContext() );
								startActivity( new Intent( AlberoNuovo.this, Alberi.class ));
							} catch( IOException e ) {
								e.printStackTrace();
							}
						}
					}).setNeutralButton( android.R.string.cancel, null )
							.create().show();
				} else
					Toast.makeText( AlberoNuovo.this, R.string.backup_invalid, Toast.LENGTH_LONG ).show();
			} catch( Exception e ) {
				Toast.makeText( AlberoNuovo.this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
			}
		}
	}

	// Confronta le date di invio degli alberi esistenti
	// Se trova almeno un albero originario tra quelli esistenti apre il confronto e restituisce true
	static boolean confronta( Context contesto, Armadio.Cassetto albero2 ) {
		if( albero2.condivisioni != null )
			for( Armadio.Cassetto alb : Globale.preferenze.alberi )
				if( alb.id != albero2.id && alb.condivisioni != null && alb.grado != 20  && alb.grado != 30 )
					for( Armadio.Invio invio : alb.condivisioni )
						for( Armadio.Invio invio2 : albero2.condivisioni )
							if( invio.data != null && invio.data.equals( invio2.data ) ) {
								Intent intento = new Intent( contesto, Compara.class );
								intento.putExtra( "idAlbero", alb.id );
								intento.putExtra( "idAlbero2", albero2.id );
								intento.putExtra( "idData", invio.data );
								contesto.startActivity( intento );
								return true;
							}
		return false;
	}

	// Crea l'intestazione standard per questa app
	public static Header creaTestata( String nomeFile ) {
		Header testa = new Header();
		Generator app = new Generator();
		app.setValue( "FAMILY_GEM" );
		app.setName( "Family Gem" );
		app.setVersion( BuildConfig.VERSION_NAME );
		testa.setGenerator( app );
		testa.setFile( nomeFile );
		GedcomVersion versione = new GedcomVersion();
		versione.setForm( "LINEAGE-LINKED" );
		versione.setVersion( "5.5.1" );
		testa.setGedcomVersion( versione );
		CharacterSet codifica = new CharacterSet();
		codifica.setValue( "UTF-8" );
		testa.setCharacterSet( codifica );
		Locale loc = new Locale( Locale.getDefault().getLanguage() );
		// C'è anche   Resources.getSystem().getConfiguration().locale.getLanguage() che ritorna lo stesso 'it'
		testa.setLanguage( loc.getDisplayLanguage( Locale.ENGLISH ) );	// ok prende la lingua di sistema in inglese, non nella lingua locale
		return testa;
	}

	// Freccia indietro nella toolbar come quella hardware
	@Override
	public boolean onOptionsItemSelected( MenuItem i ) {
		onBackPressed();
		return true;
	}
}
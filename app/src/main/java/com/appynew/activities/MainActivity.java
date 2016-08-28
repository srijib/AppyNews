package com.appynew.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.appynews.adapter.NoticiasAdapter;
import com.appynews.asynctasks.GetNoticiasAsyncTask;
import com.appynews.asynctasks.ParametrosAsyncTask;
import com.appynews.asynctasks.RespuestaAsyncTask;
import com.appynews.asynctasks.SaveUsuarioAsyncTask;
import com.appynews.com.appynews.controllers.NoticiaController;
import com.appynews.model.dto.DatosUsuarioVO;
import com.appynews.model.dto.Noticia;
import com.appynews.model.dto.OrigenNoticiaVO;
import com.appynews.utils.ConnectionUtils;
import com.appynews.utils.FileOperations;
import com.appynews.utils.LogCat;
import com.appynews.utils.LruBitmapCache;
import com.appynews.utils.MessageUtils;
import com.appynews.utils.PermissionsUtil;
import com.appynews.utils.TelephoneUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import material.oscar.com.materialdesign.R;



/**
 * Clase MainActivity que lanza el Activity Principal
 * @author oscar
 *
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView recycler;
    private RecyclerView.LayoutManager lManager;
    private int PERMISSION_ACCESS_STATE_PHONE = 1;
    private NoticiaController noticiaController = new NoticiaController(this);
    private ImageLoader imageLoader = null;

    /**
     * Contiene los datos y url de un origen RSS del que se obtendrá sus noticia
     */
    private HashMap<Integer,OrigenNoticiaVO> origenes = new HashMap<Integer,OrigenNoticiaVO>();

    /**
     * Nombre del origen de las noticias que se están listando actualmente en el RecyclerView
     */
    private String nombreOrigenNoticiasRecuperadas = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        /** Añadir elementos al menú de forma dinámica **/
        //Menu menu = navigationView.getMenu();

        rellenarMenu(navigationView);

        /*
        menu.add(Menu.CATEGORY_SYSTEM, 4, Menu.CATEGORY_SYSTEM, "Opcion1")
                .setIcon(android.R.drawable.ic_menu_preferences);
    */

        // menu.add(int groupId, int itemId, int order, int titleRes)
        //MenuItem menuItem = menu.add(Menu.NONE, 5, Menu.NONE, "Opcion2").setIcon(android.R.drawable.ic_menu_compass);

        /**
        SubMenu submenu = menu.addSubMenu(Menu.FIRST);
        submenu.add(Menu.NONE, 5, Menu.NONE, "Opcion3");
        **/



        // Obtener el Recycler
        recycler = (RecyclerView) findViewById(R.id.reciclador);
        recycler.setHasFixedSize(true);


        // Usar un administrador para LinearLayout
        lManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(lManager);


        // Si se dispone de permiso para leer el estado del teléfono, se obtiene datos como el número, imei, etc ...
        if(PermissionsUtil.appTienePermiso(this,Manifest.permission.READ_PHONE_STATE)) {
            // Recopilación de datos del dispositivo
            DatosUsuarioVO datosTelefono = TelephoneUtil.getInfoDispositivo(getBaseContext());
            LogCat.debug(" datos del telefono: " + datosTelefono.toString());

        }


        if(!PermissionsUtil.appTienePermiso(this, Manifest.permission.ACCESS_NETWORK_STATE)) {
            // Se comprueba si la app tiene permiso de acceso al estado de red del dispositivo, sino
            // se dispone del permiso, entonces se informa al usuario
            MessageUtils.showToastDuracionLarga(getApplicationContext(),getString(R.string.err_permisssion_network_state));
        } else {

            if (!ConnectionUtils.conexionRedHabilitada(this)) {
                // Si el dispositivo no tiene habilitado ninguna conexión de red, hay que informar al usuario
                MessageUtils.showToastDuracionLarga(getApplicationContext(),getString(R.string.err_connection_state));

            } else {



                /************************************************************/
                /**** Se almacenan los datos del dispositivo en la BBDD *****/
                /************************************************************/
                try {
                    ParametrosAsyncTask params = new ParametrosAsyncTask();
                    params.setContext(this.getApplicationContext());
                    params.setUsuario(TelephoneUtil.getInfoDispositivo(getApplicationContext()));
                    SaveUsuarioAsyncTask asyncTask = new SaveUsuarioAsyncTask();
                    asyncTask.execute(params);

                    RespuestaAsyncTask res = asyncTask.get();
                    if(res.getStatus()==0) {
                        LogCat.debug("Datos del teléfono/usuario grabados en BBDD");
                    } else {
                        LogCat.error("Se ha producido un error al grabar el teléfono/usuario grabados en BBDD ".concat(res.getDescStatus()));
                    }


                    GetNoticiasAsyncTask task = new GetNoticiasAsyncTask();
                    task.execute(params);
                    RespuestaAsyncTask res2 = task.get();

                    if(res2.getStatus()==0 && res2.getNoticias()!=null) {

                        for(Noticia n:res2.getNoticias()) {
                            LogCat.info("ID noticia : " + n.getId());
                            LogCat.info("titulo noticia : " + n.getTitulo());
                            LogCat.info("descripcion noticia : " + n.getDescripcion());
                            LogCat.info("descripcionCompleta noticia : " + n.getDescripcionCompleta());
                            LogCat.info("autor noticia : " + n.getAutor());
                            LogCat.info("origen noticia : " + n.getOrigen());
                            LogCat.info("fecha noticia : " + n.getFechaPublicacion());
                        }

                    } else {
                        LogCat.error("Se ha producido un error al recuperar las noticias");
                    }


                } catch (InterruptedException e) {
                    e.printStackTrace();
                    LogCat.error("Error al ejecutar tarea asíncrona de grabación de usuario en BD: ".concat(e.getMessage()));
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    LogCat.error("Error al ejecutar tarea asíncrona de grabación de usuario en BD: ".concat(e.getMessage()));
                }


                RequestQueue requestQueau = Volley.newRequestQueue(this);
                // Se crea para la cola una caché de 10 imágenes
                this.imageLoader = new ImageLoader(requestQueau,new LruBitmapCache());

                // Carga inicial de noticias de un determinado origen
                cargarNoticias("https://www.meneame.net/rss","Menéame");
            }
        }
    }


    /**
     * Operación que obtiene los datos de orígenes de datos rss y configurar el menú
     * para acceder a cada origen de datos
     * @param navigationView: NavigationView en el que se añaden los menús
     */
    private void rellenarMenu(NavigationView navigationView) {

        boolean continuar = false;
        try {
            origenes  = FileOperations.leerArchivoConfiguracion(getResources());
            continuar = true;

        }catch(Exception e) {
            MessageUtils.showToastDuracionLarga(getApplicationContext(),getString(R.string.err_fuentes_datos));
        }

        if(continuar) {

            Menu menu = navigationView.getMenu();
            // Se almacena en HashMap en un TreeMap que ordena el contenido por la clave
            Map<Integer, OrigenNoticiaVO> treeMap = new TreeMap<Integer, OrigenNoticiaVO>(origenes);

            for (Map.Entry entry : treeMap.entrySet()) {
                OrigenNoticiaVO origen = origenes.get((Integer)entry.getKey());
                MenuItem menuItem = menu.add(Menu.NONE, origen.getId(), Menu.NONE, origen.getNombre()).setIcon(android.R.drawable.ic_menu_compass);
            }
        }

    }


    /**
     * Método que carga las noticias de una determinada url
     * @param url: String que contiene la url del orígen de datos RSS
     * @param origen: String que contiene el nombre del orígen de datos RSS
     */
    private void cargarNoticias(String url,String origen) {

        this.nombreOrigenNoticiasRecuperadas = origen;

        // Se recupera la lista de noticias a través
        final List<Noticia> noticias = noticiaController.getNoticias(url);
        NoticiasAdapter adapter =  new NoticiasAdapter(noticias,origen,imageLoader,getResources());

        /**
         * Se establece el listener que se pasa al adapter para que añade
         * este Listener a cada View a mostrar en el RecyclerView
         */
        adapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos =  recycler.getChildAdapterPosition(view);
                cargarActivityDetalleNoticia(noticias.get(pos));
            }
        });

        recycler.setAdapter(adapter);
    }


    /**
     * Este método pasa el control del activity actual al activity DetalleNoticiaActivity.
     * @param noticia: Objeto de la clase Noticia que se pasa a la actividad DetalleNoticiaActivity
     */
    private void cargarActivityDetalleNoticia(Noticia noticia) {

        // Se pasa la noticia seleccionada al Activity que mostrará la descripción, en este caso, ActividadDescripcionNoticia
        Intent intent = new Intent(MainActivity.this, DetalleNoticiaActivity.class);
        noticia.setOrigen(this.nombreOrigenNoticiasRecuperadas);
        intent.putExtra("noticia",noticia);
        startActivity(intent);

    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        LogCat.debug(" ================> Se ha selecciona el elemento del menú con id: " + id);

        OrigenNoticiaVO origenSeleccionado = origenes.get(id);
        cargarNoticias(origenSeleccionado.getUrl(),origenSeleccionado.getNombre());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}

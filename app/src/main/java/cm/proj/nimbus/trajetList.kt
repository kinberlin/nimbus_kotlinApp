package cm.proj.nimbus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import cm.proj.nimbus.place.*

class trajetList : AppCompatActivity() {
    private val places: List<Place> by lazy {
        PlacesReader(this).read()
    }
    private val listTrajet : MutableList<Trajet> = mutableListOf(
        /*Trajet(places[0],places[1],"Elf Axe Lourd", "Salle des fêtes d'Akwa, Douala"),
        Trajet(places[0],places[2],"Elf Axe Lourd", "Carrefour Dallip"),
        Trajet(places[5],places[4],"Ndokoti", "Poste Centrale de Bonanjo"),
        Trajet(places[5],places[3],"Ndokoti", "Délégation Régionale Des PTT Bonanjo"),
        Trajet(places[6],places[4],"Bonabéri", "Poste Centrale de Bonanjo"),
        Trajet(places[6],places[5],"Bonabéri", "Ndokoti"),
        Trajet(places[6],places[7],"Bonabéri", "Marché Centrale de Douala"),
        Trajet(places[10],places[9],"Carrefour des douanes du Cameroun", "PK 14")*/

    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trajet_list)

       var trajet_recycle : RecyclerView = findViewById(R.id.recycle_trajet_List)
        var adapter = TrajetItemAdapter(listTrajet)
        listTrajet.add(Trajet(places[5],places[9],"Ndokoti", "PK 14"))
        trajet_recycle.adapter = adapter
        adapter.notifyDataSetChanged()

        // Applying OnClickListener to our Adapter
        adapter.setOnClickListener(object : TrajetItemAdapter.OnClickListener {
            override fun onClick(position: Int, model: Trajet) {
                val intent = Intent(this@trajetList, prelaunch_activity::class.java)
                // Passing the data to the
                // EmployeeDetails Activity
                intent.putExtra("trajet", model)
                startActivity(intent)
            }
        })

    }
}
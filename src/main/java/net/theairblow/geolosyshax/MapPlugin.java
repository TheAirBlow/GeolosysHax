package net.theairblow.geolosyshax;

import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;

@ClientPlugin
public class MapPlugin implements IClientPlugin {
    public static IClientAPI jmAPI = null;

    @Override
    public void initialize(IClientAPI jmClientApi) {
        jmAPI = jmClientApi;
    }

    @Override
    public String getModId() {
        return GeolosysHax.MOD_ID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        // We don't have anything to listen for.
    }
}

package dev.yive.pluto;

import com.destroystokyo.paper.util.VersionFetcher;
import io.papermc.paper.PaperServerInternalAPIBridge;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlutoServerInternalAPIBridge extends PaperServerInternalAPIBridge {
    public static final PlutoServerInternalAPIBridge INSTANCE = new PlutoServerInternalAPIBridge();
    @Override
    public VersionFetcher getVersionFetcher() {
        return new PlutoVersionFetcher();
    }
}

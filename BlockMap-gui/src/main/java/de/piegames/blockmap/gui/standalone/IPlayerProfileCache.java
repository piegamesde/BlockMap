package de.piegames.blockmap.gui.standalone;

import de.saibotk.jmaw.ApiResponseException;
import de.saibotk.jmaw.PlayerProfile;

import java.util.Optional;

public interface IPlayerProfileCache {
    public Optional<PlayerProfile> get(String url) throws ApiResponseException;

    public Optional<PlayerProfile> fresh(String url) throws ApiResponseException;

    public void flush();
}

package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.platform.services.IConfigHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public class FabricConfigHelper implements IConfigHelper
{
    private static final LevelResource SERVER_CONFIG_RESOURCE = new LevelResource("serverconfig");

    @Override
    public LevelResource getServerConfigResource()
    {
        return SERVER_CONFIG_RESOURCE;
    }

    @Override
    public Set<IModConfigProvider> getProviders()
    {
        Set<IModConfigProvider> providers = new HashSet<>();
        this.readProviders(obj ->
        {
            if(obj instanceof IModConfigProvider provider)
            {
                providers.add(provider);
            }
        });
        return providers;
    }

    private void readProviders(Consumer<Object> function)
    {
        FabricLoader.getInstance().getAllMods().forEach(container ->
        {
            CustomValue value = container.getMetadata().getCustomValue("configured");
            if(value != null && value.getType() == CustomValue.CvType.OBJECT)
            {
                CustomValue.CvObject configuredObj = value.getAsObject();
                CustomValue providersValue = configuredObj.get("providers");
                if(providersValue != null)
                {
                    if(providersValue.getType() == CustomValue.CvType.ARRAY)
                    {
                        CustomValue.CvArray array = providersValue.getAsArray();
                        array.forEach(providerValue -> this.readProvider(providerValue, container, function));
                    }
                    else
                    {
                        this.readProvider(providersValue, container, function);
                    }
                }
            }
        });
    }

    private void readProvider(CustomValue providerValue, ModContainer container, Consumer<Object> function)
    {
        if(providerValue.getType() == CustomValue.CvType.STRING)
        {
            String providerClass = providerValue.getAsString();
            Object obj = this.createProviderInstance(container, providerClass);
            function.accept(obj);
            Constants.LOG.info("Successfully loaded config provider: {}", providerClass);
        }
        else
        {
            throw new RuntimeException("Config provider definition must be a String");
        }
    }

    private Object createProviderInstance(ModContainer container, String classPath)
    {
        try
        {
            Class<?> providerClass = Class.forName(classPath);
            Object obj = providerClass.getDeclaredConstructor().newInstance();
            if(!(obj instanceof IModConfigProvider))
            {
                throw new RuntimeException("Config providers must implement IModConfigProvider");
            }
            return obj;
        }
        catch(Exception e)
        {
            Constants.LOG.error("Failed to load config provider from mod: {}", container.getMetadata().getId());
            throw new RuntimeException("Failed to load config provider", e);
        }
    }
}

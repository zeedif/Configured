package com.mrcrayfish.configured.impl.neoforge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.ValueEntry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class NeoForgeFolderEntry implements IConfigEntry
{
    protected final List<String> path;
    protected final UnmodifiableConfig config;
    protected final ModConfigSpec spec;
    protected List<IConfigEntry> entries;

    public NeoForgeFolderEntry(UnmodifiableConfig config, ModConfigSpec spec)
    {
        this(new ArrayList<>(), config, spec);
    }

    public NeoForgeFolderEntry(List<String> path, UnmodifiableConfig config, ModConfigSpec spec)
    {
        this.path = path;
        this.config = config;
        this.spec = spec;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<IConfigEntry> getChildren()
    {
        if(this.entries == null)
        {
            ImmutableList.Builder<IConfigEntry> builder = ImmutableList.builder();
            this.config.valueMap().forEach((s, o) ->
			{
                if(o instanceof UnmodifiableConfig)
                {
                    List<String> path = new ArrayList<>(this.path);
                    path.add(s);
                    builder.add(new NeoForgeFolderEntry(path, (UnmodifiableConfig) o, this.spec));
                }
                else if(o instanceof ModConfigSpec.ConfigValue<?> configValue)
                {
                    if(configValue.get() instanceof List<?>)
                    {
                        builder.add(new ValueEntry(new NeoForgeListValue(configValue, this.spec.getRaw(configValue.getPath()))));
                    }
                    else if(configValue.get() instanceof Enum<?>)
                    {
                        builder.add(new ValueEntry(new NeoForgeEnumValue<>((ModConfigSpec.EnumValue<?>) configValue, this.spec.getRaw(configValue.getPath()))));
                    }
                    else
                    {
                        builder.add(new ValueEntry(new NeoForgeValue<>(configValue, this.spec.getRaw(configValue.getPath()))));
                    }
                }
            });
            this.entries = builder.build();
        }
        return this.entries;
    }

    @Override
    public boolean isRoot()
    {
        return this.path.isEmpty();
    }

    @Override
    public boolean isLeaf()
    {
        return false;
    }

    @Override
    public IConfigValue<?> getValue()
    {
        return null;
    }

    @Override
    public String getEntryName()
    {
        return NeoForgeValue.lastValue(this.path, "Root");
    }

    @Nullable
    @Override
    public Component getTooltip()
    {
        String translationKey = this.getTranslationKey();
        if(translationKey != null)
        {
            String tooltipKey = translationKey + ".tooltip";
            if(I18n.exists(tooltipKey))
            {
                return Component.translatable(tooltipKey);
            }
        }
        String comment = this.spec.getLevelComment(this.path);
        if(comment != null)
        {
            return Component.literal(comment);
        }
        return null;
    }

    @Nullable
    @Override
    public String getTranslationKey()
    {
        return this.spec.getLevelTranslationKey(this.path);
    }
}
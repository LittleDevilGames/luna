package io.luna.game.model.def;

import com.google.common.collect.ImmutableList;
import io.luna.util.StringUtils;
import io.luna.util.parser.impl.NpcDefinitionParser;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * A definition model describing a non-player.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class NpcDefinition {

    /**
     * A list of non-player definitions.
     */
    public static final ImmutableList<NpcDefinition> DEFINITIONS;

    /**
     * A default definition. Used as a substitute for {@code null}.
     */
    private static final NpcDefinition DEFAULT = new NpcDefinition(-1, null, null, -1, -1, -1, -1, -1,
        StringUtils.EMPTY_ARRAY);

    /**
     * Retrieves the definition for {@code id}.
     */
    public static NpcDefinition get(int id) {
        NpcDefinition def = DEFINITIONS.get(id);
        if (def == DEFAULT) {
            throw new NoSuchElementException("No definition for id " + id);
        }
        return def;
    }

    /**
     * Returns an iterable containing all definitions.
     */
    public static Iterable<NpcDefinition> all() {
        return DEFINITIONS;
    }

    /**
     * Returns the non-player name of {@code id}.
     */
    public static String computeNameForId(int id) {
        return get(id).getName();
    }

    static { /* Populate the immutable list with definitions. */
        NpcDefinition[] definitions = new NpcDefinition[8152];
        Arrays.fill(definitions, DEFAULT);

        NpcDefinitionParser parser = new NpcDefinitionParser(definitions);
        parser.run();

        DEFINITIONS = ImmutableList.copyOf(definitions);
    }

    /**
     * The identifier.
     */
    private final int id;

    /**
     * The name.
     */
    private final String name;

    /**
     * The examine text.
     */
    private final String examine;

    /**
     * The size.
     */
    private final int size;

    /**
     * The walking animation.
     */
    private final int walkAnimation;

    /**
     * The walking-back animation.
     */
    private final int walkBackAnimation;

    /**
     * The walking-left animation.
     */
    private final int walkLeftAnimation;

    /**
     * The walking-right animation.
     */
    private final int walkRightAnimation;

    /**
     * A list of actions.
     */
    private final ImmutableList<String> actions;

    /**
     * Creates a new {@link NpcDefinition}.
     *
     * @param id The identifier.
     * @param name The name.
     * @param examine The examine text.
     * @param size The size.
     * @param walkAnimation The walking animation.
     * @param walkBackAnimation The walking-back animation.
     * @param walkLeftAnimation The walking-left animation.
     * @param walkRightAnimation The walking-right animation.
     * @param actions A list of actions.
     */
    public NpcDefinition(int id, String name, String examine, int size, int walkAnimation, int walkBackAnimation,
        int walkLeftAnimation, int walkRightAnimation, String[] actions) {
        this.id = id;
        this.name = name;
        this.examine = examine;
        this.size = size;
        this.walkAnimation = walkAnimation;
        this.walkBackAnimation = walkBackAnimation;
        this.walkLeftAnimation = walkLeftAnimation;
        this.walkRightAnimation = walkRightAnimation;
        this.actions = ImmutableList.copyOf(actions);
    }

    /**
     * Determines if {@code action} is an action.
     */
    public boolean hasAction(String action) {
        return actions.contains(action);
    }

    /**
     * @return The identifier.
     */
    public int getId() {
        return id;
    }

    /**
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The examine text.
     */
    public String getExamine() {
        return examine;
    }

    /**
     * @return The size.
     */
    public int getSize() {
        return size;
    }

    /**
     * @return The walking animation.
     */
    public int getWalkAnimation() {
        return walkAnimation;
    }

    /**
     * @return The walking-back animation.
     */
    public int getWalkBackAnimation() {
        return walkBackAnimation;
    }

    /**
     * @return The walking-left animation.
     */
    public int getWalkLeftAnimation() {
        return walkLeftAnimation;
    }

    /**
     * @return The walking-right animation.
     */
    public int getWalkRightAnimation() {
        return walkRightAnimation;
    }

    /**
     * @return A list of actions.
     */
    public ImmutableList<String> getActions() {
        return actions;
    }
}

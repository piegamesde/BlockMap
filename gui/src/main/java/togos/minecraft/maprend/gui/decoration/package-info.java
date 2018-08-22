/**
 * <p>
 * A Decoration is a {@link javafx.scene.Node} (typically a subclass of {@link javafx.scene.layout.Region}), that is part of the internal stack of the
 * {@link togos.minecraft.maprend.gui.MapPane}. Multiple decorations are added on top of each other and may provide additional functionality or visual
 * information to the world map. They all have the same size as the map. There are two types of Decorations:
 * <ul>
 * <li>A settings layer is a normal GUI layer on top that provides additional functionality. The <code>pickOnBounds</code> property should be set on false so
 * that events only get caught where there actually is some GUI beneath. Otherwise user interaction events with the map might not reach their goal.</li>
 * <li>A decoration layer does not catch any events. All events that would reach it get caught by an internal component and distributed to all decoration
 * layers. This allows to have multiple decorations that use the same events, e.g. one for dragging and one for clicking or using different mouse buttons.
 * Decoration layers thus cannot easily contain conventional GUI.</li>
 * </ul>
 * </p>
 * <p>
 * Available decorations:
 * <ul>
 * <li>{@link togos.minecraft.maprend.gui.decoration.SettingsOverlay}: Puts a hideable panel at the right of the map to change the min and max height setting.
 * </li>
 * <li>{@link togos.minecraft.maprend.gui.decoration.DragScrollDecoration}: Provides basic drag and zoom functionality.</li>
 * </ul>
 * What is yet to come: More settings, different decorations for selecting areas, showing points (players, monuments, etc.) on the map
 * </p>
 * 
 * @author piegames
 */
package togos.minecraft.maprend.gui.decoration;
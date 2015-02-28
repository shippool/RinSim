/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.ui.renderers;

import static com.github.rinde.rinsim.ui.renderers.PointUtil.angle;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Transform;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModelEvent;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.Factory;
import com.github.rinde.rinsim.ui.IBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * Renders vehicles as AGVs. Instances can be obtained via
 * {@link AGVRenderer#builder()}.
 * <p>
 * <b>Requires:</b> a {@link CollisionGraphRoadModel} in the
 * {@link com.github.rinde.rinsim.core.Simulator}.
 * @author Rinde van Lon
 */
public final class AGVRenderer implements CanvasRenderer, Listener {
  private static final int DEFAULT_COLOR = SWT.COLOR_BLACK;
  private final CollisionGraphRoadModel model;
  private final RenderHelper helper;
  private final Map<MovingRoadUser, VehicleUI> vehicles;
  private final boolean useDifferentColors;
  private int vehicleCounter;

  enum DebugStringType {
    COORDINATES, CREATION_NUMBER;
  }

  private final ImmutableSet<DebugStringType> labelTypes;

  private final Iterator<Integer> colors = Iterators.cycle(SWT.COLOR_BLUE,
      SWT.COLOR_RED, SWT.COLOR_GREEN, SWT.COLOR_CYAN, SWT.COLOR_MAGENTA,
      SWT.COLOR_YELLOW, SWT.COLOR_DARK_BLUE, SWT.COLOR_DARK_RED,
      SWT.COLOR_DARK_GREEN, SWT.COLOR_DARK_CYAN, SWT.COLOR_DARK_MAGENTA,
      SWT.COLOR_DARK_YELLOW);

  AGVRenderer(CollisionGraphRoadModel m, AGVRendererFactory factory) {
    model = m;
    helper = new RenderHelper();
    vehicles = new LinkedHashMap<>();
    useDifferentColors = factory.getUseDifferentColorsForVehicles();
    vehicleCounter = 0;
    labelTypes = factory.getDebugStringTypes();

    final Set<RoadUser> obs = model.getObjects();
    for (final RoadUser ru : obs) {
      if (ru instanceof MovingRoadUser) {
        addVehicleUI((MovingRoadUser) ru);
      }
    }

    model.getEventAPI().addListener(this,
        RoadEventType.ADD_ROAD_USER,
        RoadEventType.REMOVE_ROAD_USER);
  }

  void addVehicleUI(MovingRoadUser mru) {
    final int color = useDifferentColors ? colors.next() : DEFAULT_COLOR;
    final VehicleUI v = new VehicleUI(mru, model, color, labelTypes,
        vehicleCounter++);

    verify(vehicles.put(mru, v) == null);
  }

  @Override
  public void handleEvent(Event e) {
    verify(e instanceof RoadModelEvent);
    final RoadModelEvent rme = (RoadModelEvent) e;
    if (rme.roadUser instanceof MovingRoadUser) {
      if (e.getEventType() == RoadEventType.ADD_ROAD_USER) {
        addVehicleUI((MovingRoadUser) rme.roadUser);
      } else if (e.getEventType() == RoadEventType.REMOVE_ROAD_USER) {
        verifyNotNull(vehicles.remove(rme.roadUser)).dispose();
      } else {
        verify(false);
      }
    }
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {}

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {
    helper.adapt(gc, vp);
    for (final VehicleUI v : vehicles.values()) {
      v.update(gc, vp, helper);
    }
  }

  @Override
  @Nullable
  public ViewRect getViewRect() {
    return null;
  }

  /**
   * @return A {@link Builder} for creating a {@link AGVRenderer}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating {@link AGVRenderer}s.
   * @author Rinde van Lon
   */
  public static class Builder implements
      IBuilder<Factory<AGVRenderer, ModelProvider>> {
    boolean useDifferentColors;
    Set<DebugStringType> debugStringTypes;

    Builder() {
      useDifferentColors = false;
      debugStringTypes = EnumSet.noneOf(DebugStringType.class);
    }

    /**
     * Draws a number on each vehicle. The number indicates the creation order
     * of the vehicle.
     * @return This, as per the builder pattern.
     */
    public Builder showVehicleCreationNumber() {
      debugStringTypes.add(DebugStringType.CREATION_NUMBER);
      return this;
    }

    /**
     * Displays the coordinates of each vehicle next to it.
     * @return This, as per the builder pattern.
     */
    public Builder showVehicleCoordinates() {
      debugStringTypes.add(DebugStringType.COORDINATES);
      return this;
    }

    /**
     * Vehicles are drawn with different colors to ease debugging.
     * @return This, as per the builder pattern.
     */
    public Builder useDifferentColorsForVehicles() {
      useDifferentColors = true;
      return this;
    }

    @Override
    public Factory<AGVRenderer, ModelProvider> build() {
      return AGVRendererFactory.create(this);
    }
  }

  @AutoValue
  abstract static class AGVRendererFactory implements
      Factory<AGVRenderer, ModelProvider> {

    AGVRendererFactory() {}

    abstract boolean getUseDifferentColorsForVehicles();

    abstract ImmutableSet<DebugStringType> getDebugStringTypes();

    @Override
    public AGVRenderer create(ModelProvider argument) {
      return new AGVRenderer(argument.getModel(CollisionGraphRoadModel.class),
          this);
    }

    static Factory<AGVRenderer, ModelProvider> create(Builder b) {
      return new AutoValue_AGVRenderer_AGVRendererFactory(b.useDifferentColors,
          Sets.immutableEnumSet(b.debugStringTypes));
    }
  }

  static class VehicleUI {
    final CollisionGraphRoadModel model;
    final MovingRoadUser vehicle;
    Point position;
    double angle;
    Optional<? extends Connection<?>> connection;

    final int color;
    final Set<DebugStringType> labelTypes;
    final int creationNumber;
    double scale = 1;
    Optional<Image> image;

    VehicleUI(MovingRoadUser mru, CollisionGraphRoadModel m, int c,
        Set<DebugStringType> t, int num) {
      vehicle = mru;
      model = m;
      angle = 0;
      connection = Optional.absent();
      color = c;
      labelTypes = t;
      creationNumber = num;
      position = new Point(0, 0);
      image = Optional.absent();
    }

    void dispose() {
      if (image.isPresent()) {
        image.get().dispose();
      }
    }

    Image createImage(GC gc, ViewPort vp) {
      if (image.isPresent()) {
        image.get().dispose();
      }
      final int length = (int) (model.getVehicleLength() * vp.scale);
      final int width = length / 2;
      final int frontSize = length / 8;
      final Image img = new Image(gc.getDevice(), width, length);
      final GC igc = new GC(img);

      igc.setBackground(gc.getDevice().getSystemColor(color));
      igc.fillPolygon(new int[] {
          frontSize, 0,
          width - frontSize, 0,
          width, frontSize,
          0, frontSize
      });
      igc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
      igc.fillRectangle(0, frontSize, width, length - frontSize);

      if (labelTypes.contains(DebugStringType.CREATION_NUMBER)) {
        final String string = Integer.toString(creationNumber);
        final double factor = width / (double) igc.stringExtent(string).x;
        final Font initialFont = igc.getFont();
        final FontData[] fontData = initialFont.getFontData();
        for (int i = 0; i < fontData.length; i++) {
          fontData[i].setHeight((int) (fontData[i].getHeight() * factor));
        }
        final Font newFont = new Font(gc.getDevice(), fontData);
        igc.setFont(newFont);

        final org.eclipse.swt.graphics.Point finalTextSize = igc
            .stringExtent(string);

        final int xOffset = (int) ((width - finalTextSize.x) / 2d);
        final int yOffset = frontSize
            + (int) ((length - frontSize - finalTextSize.y) / 2d);
        igc.drawText(string, xOffset, yOffset, true);
        newFont.dispose();
      }
      igc.dispose();
      return img;
    }

    void update(GC gc, ViewPort vp, RenderHelper helper) {
      position = model.getPosition(vehicle);
      final Optional<? extends Connection<?>> conn = model
          .getConnection(vehicle);

      if (!image.isPresent() || scale != vp.scale) {
        scale = vp.scale;
        image = Optional.of(createImage(gc, vp));
      }

      if (conn.isPresent()) {
        connection = conn;
        angle = angle(conn.get());
      }

      final int x = vp.toCoordX(position.x);
      final int y = vp.toCoordY(position.y);

      final Transform transform = new Transform(gc.getDevice());
      transform.translate(x, y);
      transform.rotate((float) (90 + angle * 180 / Math.PI));
      transform.translate(
          -(x + image.get().getBounds().width / 2),
          -y);
      gc.setTransform(transform);
      gc.drawImage(image.get(), x, y);
      gc.setTransform(null);
      transform.dispose();

      if (labelTypes.contains(DebugStringType.COORDINATES)) {
        helper.setBackgroundSysCol(SWT.COLOR_YELLOW);
        helper.setForegroundSysCol(SWT.COLOR_BLACK);
        gc.drawString(String.format("%1.2f,%1.2f", position.x, position.y),
            vp.toCoordX(position.x),
            vp.toCoordY(position.y));
      }
    }
  }
}

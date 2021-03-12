package de.mig.mdr.model.handler.element.section;

import static de.mig.mdr.dal.jooq.Tables.ELEMENT;
import static de.mig.mdr.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.mig.mdr.dal.jooq.tables.Slot.SLOT;

import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.dal.jooq.Tables;
import de.mig.mdr.dal.jooq.tables.Element;
import de.mig.mdr.model.CtxUtil;
import de.mig.mdr.model.dto.element.section.Slot;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;

public class SlotHandler {

  /**
   * Get slots for an identifier.
   */
  public static List<Slot> get(DSLContext ctx, Identification identifier) {
    de.mig.mdr.dal.jooq.tables.Slot slot = Tables.SLOT.as("slot");
    de.mig.mdr.dal.jooq.tables.ScopedIdentifier si = SCOPED_IDENTIFIER.as("si");
    Element ns = ELEMENT.as("ns");

    List<de.mig.mdr.dal.jooq.tables.pojos.Slot> slots =
        ctx.select()
            .from(slot)
            .leftJoin(si).on(slot.SCOPED_IDENTIFIER_ID.equal(si.ID))
            .leftJoin(ns).on(ns.ID.eq(si.NAMESPACE_ID))
            .where(si.IDENTIFIER.eq(identifier.getIdentifier()))
            .and(si.VERSION.eq(identifier.getRevision()))
            .and(si.ELEMENT_TYPE.eq(identifier.getElementType()))
            .and(si.NAMESPACE_ID.eq(identifier.getNamespaceId()))
            .fetchInto(slot).into(de.mig.mdr.dal.jooq.tables.pojos.Slot.class);

    return convert(slots);
  }


  /**
   * Get a Slot.
   */
  public static List<Slot> get(DSLContext ctx, String urn) {
    return get(ctx, IdentificationHandler.fromUrn(urn));
  }

  /**
   * Convert a List of Slot POJOs from MDR DAL to a List of Slot object of MDR Model.
   */
  public static List<Slot> convert(List<de.mig.mdr.dal.jooq.tables.pojos.Slot> slotPojos) {
    return slotPojos.stream().map(SlotHandler::convert).collect(Collectors.toList());
  }

  /**
   * Convert a Slot POJO from MDR DAL to a Slot object of MDR Model.
   */
  public static Slot convert(de.mig.mdr.dal.jooq.tables.pojos.Slot slot) {
    Slot newSlot = new Slot();
    newSlot.setName(slot.getKey());
    newSlot.setValue(slot.getValue());
    return newSlot;
  }

  /**
   * Convert a Slot object of MDR Model to a Slot object of MDR DAL.
   */
  public static de.mig.mdr.dal.jooq.tables.pojos.Slot convert(Slot slot,
      int scopedIdentifierId) {
    de.mig.mdr.dal.jooq.tables.pojos.Slot newSlot
        = new de.mig.mdr.dal.jooq.tables.pojos.Slot();
    newSlot.setKey(slot.getName());
    newSlot.setValue(slot.getValue());
    newSlot.setScopedIdentifierId(scopedIdentifierId);
    return newSlot;
  }

  /**
   * Convert a list of slots.
   */
  public static List<de.mig.mdr.dal.jooq.tables.pojos.Slot> convert(List<Slot> slots,
      int scopedIdentifierId) {
    return slots.stream().map(s -> convert(s, scopedIdentifierId)).collect(Collectors.toList());
  }

  /**
   * Create a list of Slots.
   */
  public static void create(DSLContext ctx, List<Slot> slots, int scopedIdentifierId) {
    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    List<de.mig.mdr.dal.jooq.tables.pojos.Slot> dalSlots = SlotHandler
        .convert(slots, scopedIdentifierId);
    saveSlots(ctx, dalSlots);
    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
  }

  /**
   * Save a list of slots.
   */
  public static void saveSlots(DSLContext ctx,
      List<de.mig.mdr.dal.jooq.tables.pojos.Slot> slots) {
    slots.forEach(d -> ctx.newRecord(SLOT, d).store());
  }

  /**
   * Save a slot.
   */
  public static void saveSlot(DSLContext ctx,
      de.mig.mdr.dal.jooq.tables.pojos.Slot slot) {
    ctx.newRecord(SLOT, slot).store();
  }
}

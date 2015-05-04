/*=========================================================================
 * Copyright (c) 2002-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
   
   
package com.gemstone.gemfire.internal.admin.remote;

import com.gemstone.gemfire.distributed.internal.*;
import com.gemstone.gemfire.internal.i18n.LocalizedStrings;
import com.gemstone.gemfire.*;
//import com.gemstone.gemfire.internal.*;
import java.io.*;
//import java.util.*;

/**
 * A message that is sent to a particular app vm to request the value, stats,
 * and attributes of a given region entry.
 */
public final class ObjectDetailsRequest extends RegionAdminRequest implements Cancellable {
  // instance variables
  private Object objName;
  private int inspectionType;
  private transient boolean cancelled;
  private ObjectDetailsResponse resp;

  /**
   * Returns a <code>ObjectValueRequest</code> to be sent to the specified recipient.
   */
  public static ObjectDetailsRequest create(Object objName, int inspectionType) {
    ObjectDetailsRequest m = new ObjectDetailsRequest();
    m.objName = objName;
    m.inspectionType = inspectionType;
    return m;
  }

  public ObjectDetailsRequest() {
    friendlyName = LocalizedStrings.ObjectDetailsRequest_INSPECT_CACHED_OBJECT.toLocalizedString();
  }

  public synchronized void cancel() {
    cancelled = true;
    if (resp != null) {
      resp.cancel();
    }
  }

  /**
   * Must return a proper response to this request.
   */
  @Override
  protected AdminResponse createResponse(DistributionManager dm) {
    CancellationRegistry.getInstance().registerMessage(this);
    resp = ObjectDetailsResponse.create(dm, this.getSender());
    if (cancelled) {
      return null;
    }
    resp.buildDetails(this.getRegion(dm.getSystem()),
                      this.objName, this.inspectionType);
    if (cancelled) {
      return null;
    }
    CancellationRegistry.getInstance().deregisterMessage(this);
    return resp;
  }

  public int getDSFID() {
    return OBJECT_DETAILS_REQUEST;
  }

  @Override
  public void toData(DataOutput out) throws IOException {
    super.toData(out);
    DataSerializer.writeObject(this.objName, out);
    out.writeInt(inspectionType);
  }

  @Override
  public void fromData(DataInput in)
    throws IOException, ClassNotFoundException {
    super.fromData(in);
    this.objName = DataSerializer.readObject(in);
    this.inspectionType = in.readInt();
  }

  @Override
  public String toString() {
    return "ObjectDetailsRequest from " + getRecipient() + " region=" +
      getRegionName() + " object=" + this.objName;
  }
}
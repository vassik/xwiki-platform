/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 22 d�c. 2003
 * Time: 09:19:06
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.ListClass;

public class NumberMetaClass extends PropertyMetaClass {

  public NumberMetaClass() {
    super();
    // setType("numbermetaclass");
    setPrettyName("Number Class");
    setName(NumberClass.class.getName());

    StaticListClass type_class = new StaticListClass(this);
    type_class.setName("numberType");
    type_class.setPrettyName("Number Type");
    type_class.setValues("integer|long|float|double");
    type_class.setRelationalStorage(false);
    type_class.setDisplayType("select");
    type_class.setMultiSelect(false);
    type_class.setSize(1);
    safeput("numberType", type_class);

    NumberClass size_class = new NumberClass(this);
    size_class.setName("size");
    size_class.setPrettyName("Size");
    size_class.setSize(5);
    size_class.setNumberType("integer");

    safeput("numberType", type_class);
    safeput("size", size_class);
  }

  public BaseCollection newObject() {
        return new NumberClass();
  }
}

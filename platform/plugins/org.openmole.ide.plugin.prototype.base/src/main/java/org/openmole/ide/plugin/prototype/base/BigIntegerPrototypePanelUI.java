/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.plugin.prototype.base;

import org.openmole.ide.core.properties.IPanelUIData;
import org.openmole.ide.core.properties.PanelUI;
import org.openmole.ide.plugin.prototype.base.BigIntegerPrototypePanelUIData;

/**
 *
 * @author mathieu
 */
public class BigIntegerPrototypePanelUI extends PanelUI {
    
    public IPanelUIData saveContent(String name) {
        return new BigIntegerPrototypePanelUIData(name);
    }

    public void loadContent(IPanelUIData pud) {
    }
    
}

/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.workflow

import scala.collection.mutable.HashMap
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet

class MoleSceneManager(var startingCapsule: Option[ICapsuleUI]= None) extends IMoleSceneManager{
  
  var capsules= new DualHashBidiMap[String, ICapsuleUI]
  var transitionMap= new DualHashBidiMap[String, ITransitionUI]
  var dataChannelMap = new DualHashBidiMap[String, IDataChannelUI]
  var capsuleConnections= new HashMap[ICapsuleUI, HashSet[ITransitionUI]]
  var nodeID = 0
  var edgeID = 0
  var dataChannelID = 0
  var name: Option[String]= None
  
  override def setStartingCapsule(stCapsule: ICapsuleUI) = {
    startingCapsule match {
      case Some(x: ICapsuleUI)=> x.defineAsStartingCapsule(false)
      case None=>
    }
    startingCapsule= Some(stCapsule)
    startingCapsule.get.defineAsStartingCapsule(true)
  }
  
  def getNodeID: String= "node" + nodeID
  
  def getEdgeID: String= "edge" + edgeID
  
  def getDataChannelID: String= "dc" + dataChannelID
  
  override def registerCapsuleUI(cv: ICapsuleUI) = {
    nodeID+= 1
    capsules.put(getNodeID,cv)
    if (capsules.size == 1) {
      startingCapsule= Some(cv)}
    capsuleConnections+= cv-> HashSet.empty[ITransitionUI]
  }
  
  def removeCapsuleUI(nodeID: String) = {
    startingCapsule match {
      case None=>
      case Some(caps)=> if (capsules.get(nodeID) == caps) startingCapsule = None
    }
    
    //remove following transitionMap
    capsuleConnections(capsules.get(nodeID)).foreach{x=>transitionMap.removeValue(x)}
    capsuleConnections-= capsules.get(nodeID)
    
    //remove incoming transitionMap
    removeIncomingTransitions(capsules.get(nodeID))
    removeDataChannel(capsules.get(nodeID))
    
    capsules.remove(nodeID)
  }
  
  
  def capsuleID(cv: ICapsuleUI) = capsules.getKey(cv)
  
  def transitions= transitionMap.values 
  
  def dataChannels= dataChannelMap.values
  
  def transition(edgeID: String) = transitionMap.get(edgeID)
  
  private def removeIncomingTransitions(capsule: ICapsuleUI) = transitionMap.foreach(t => {if (t._2.target.capsule.equals(capsule)) {
        removeTransition(t._1)
        capsuleConnections(t._2.source)-= t._2    
      }
    })
  
  
  def removeTransition(edge: String) = transitionMap.remove(edge)
  
  def removeDataChannel(id: String) : Unit = dataChannelMap.remove(id)
  
  def removeDataChannel(capsule: ICapsuleUI) : Unit = {
    dataChannelMap.foreach{case(k,v)=> if (v.source == capsule || v.target == capsule) removeDataChannel(k)}
  }
  
  def registerDataChannel(source: ICapsuleUI, target: ICapsuleUI) : Boolean = {
    dataChannelID+= 1
    registerDataChannel(getDataChannelID, source, target)
  }
  
  def registerDataChannel(id: String,source: ICapsuleUI, target: ICapsuleUI) : Boolean = {
    if (!dataChannelMap.keys.contains(id)) {dataChannelMap.put(id,new DataChannelUI(source,target));return true}
    false
  }
  
  def registerTransition(s: ICapsuleUI, t:IInputSlotWidget,transitionType: TransitionType.Value,cond: Option[String]): Boolean = {
    edgeID+= 1
    registerTransition(getEdgeID,s,t,transitionType,cond)
  }
  
  def registerTransition(edgeID: String,s: ICapsuleUI, t:IInputSlotWidget,transitionType: TransitionType.Value,cond: Option[String]): Boolean = {
    if (!transitionMap.keys.contains(edgeID)) { 
      val transition = new TransitionUI(s,t,transitionType,cond)
      transitionMap.put(edgeID, transition)
      capsuleConnections(transition.source)+= transition
      return true
    }
    false
  }
}

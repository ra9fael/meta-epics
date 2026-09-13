# SPDX-FileCopyrightText: 2026 IMPCAS
#
# SPDX-License-Identifier: MIT

TOP=../..

include $(TOP)/configure/CONFIG
#----------------------------------------
#  ADD MACRO DEFINITIONS AFTER THIS LINE
#=============================

# Build the IOC application
PROD_IOC = demoIoc
DBD += demoIoc.dbd

# demoIoc.dbd will be made up from these files:
demoIoc_DBD += base.dbd
# asyn driver: drvAsynIPPortConfigure
demoIoc_DBD += asyn.dbd
demoIoc_DBD += drvAsynIPPort.dbd
# autosave: save_restore / create_monitor_set
demoIoc_DBD += asSupport.dbd

# Support libraries. asyn.dbd also declares the asynOctetWriteRead device
# support used by db/test.db.
demoIoc_LIBS += asyn
demoIoc_LIBS += autosave

# demoIoc_registerRecordDeviceDriver.cpp derives from demoIoc.dbd
demoIoc_SRCS += demoIoc_registerRecordDeviceDriver.cpp

# Build the main IOC entry point on workstation OSs.
demoIoc_SRCS_DEFAULT += demoIocMain.cpp
demoIoc_SRCS_vxWorks += -nil-

# Finally link to the EPICS Base libraries
demoIoc_LIBS += $(EPICS_BASE_IOC_LIBS)

#===========================

include $(TOP)/configure/RULES

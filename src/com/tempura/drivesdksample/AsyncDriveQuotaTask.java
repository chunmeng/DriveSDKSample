/*
 * Copyright (c) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tempura.drivesdksample;

import com.google.api.services.drive.model.About;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Asynchronously load the tasks.
 * 
 * @author Yaniv Inbar
 */
class AsyncDriveQuotaTask extends CommonAsyncTask {

    AsyncDriveQuotaTask(DriveSample driveSample) {
    super(driveSample);
  }

  @Override
  protected void doInBackground() throws IOException {
    About about = activity.service.about().get().execute();
    
    activity.mUsername = about.getName();
    activity.mTotalQuota = about.getQuotaBytesTotal();
    activity.mUsedQuota = about.getQuotaBytesUsed();
  }

  static void run(DriveSample driveSample) {
    new AsyncDriveQuotaTask(driveSample).execute();
  }
}

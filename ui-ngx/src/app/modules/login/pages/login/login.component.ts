///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { AfterContentInit, AfterViewInit, Component, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Constants } from '@shared/models/constants';
import { Router } from '@angular/router';
import { OAuth2ClientLoginInfo } from '@shared/models/oauth2.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent extends PageComponent implements OnInit , AfterContentInit{
  logo = 'https://matdash-angular-main.netlify.app/assets/images/logos/logo.svg';
  aText = [
    this.translate.instant("signup.welcome-message")
  ];
  iSpeed = 50;
  iIndex = 0;
  iArrLength = this.aText[0].length;
  iScrollAt = 20;
  start = false;
  iTextPos = 0;
  sContents = '';
  iRow: number;


  passwordViolation = false;

  loginFormGroup = this.fb.group({
    username: '',
    password: ''
  });
  oauth2Clients: Array<OAuth2ClientLoginInfo> = null;

  constructor(protected store: Store<AppState>,
              private authService: AuthService,
              public fb: UntypedFormBuilder,
              private router: Router, 
              private translate: TranslateService) {
    super(store);
  }

  ngAfterContentInit(): void {
     setTimeout(() => {
      this.typewriter();
    }, 1000);
  }
  ngOnInit() {
    this.oauth2Clients = this.authService.oauth2Clients;
  }

   typewriter() {
    this.sContents = ' ';
    this.iRow = Math.max(0, this.iIndex - this.iScrollAt);
    const destination = document.getElementById('typedtext');

    while (this.iRow < this.iIndex) {
      this.sContents += this.aText[this.iRow++] + '<br />';
    }

    if (destination) {
      destination.innerHTML = this.sContents + this.aText[this.iIndex].substring(0, this.iTextPos) + '|';
    }

    if (this.iTextPos++ === this.iArrLength) {
      this.iTextPos = 0;
      this.iIndex++;
      if (this.iIndex !== this.aText.length) {
        this.iArrLength = this.aText[this.iIndex].length;
        setTimeout(() => this.typewriter(), 500);
      }
     this.start = true;
    } else {
      setTimeout(() => this.typewriter(), this.iSpeed);
    }
  }

  login(): void {
    if (this.loginFormGroup.valid) {
      this.authService.login(this.loginFormGroup.value).subscribe(
        () => {},
        (error: HttpErrorResponse) => {
          if (error && error.error && error.error.errorCode) {
            if (error.error.errorCode === Constants.serverErrorCode.credentialsExpired) {
              this.router.navigateByUrl(`login/resetExpiredPassword?resetToken=${error.error.resetToken}`);
            } else if (error.error.errorCode === Constants.serverErrorCode.passwordViolation) {
              this.passwordViolation = true;
            }
          }
        }
      );
    } else {
      Object.keys(this.loginFormGroup.controls).forEach(field => {
        const control = this.loginFormGroup.get(field);
        control.markAsTouched({onlySelf: true});
      });
    }
  }

  getOAuth2Uri(oauth2Client: OAuth2ClientLoginInfo): string {
    let result = "";
    if (this.authService.redirectUrl) {
      result += "?prevUri=" + this.authService.redirectUrl;
    }
    return oauth2Client.url + result;
  }
}

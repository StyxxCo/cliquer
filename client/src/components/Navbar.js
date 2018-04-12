import React, { Component } from 'react'
import { Button, Navbar, NavbarBrand, Nav, NavItem, NavLink,
         InputGroup, InputGroupAddon, InputGroupButtonDropdown,
         Input, DropdownToggle, DropdownMenu, DropdownItem, Collapse, 
         NavbarToggler, Col } from 'reactstrap'

import '../css/Navbar.css'
import Logo from '../img/cliquerLogo-sm2.png'
import { auth } from '../firebase'
import { history } from '../redux/store'

class NavigationBar extends Component {
  constructor(props) {
    super(props)

    this.state = {
      splitButtonOpen: false,
      current: 'First Name',
      url: 'firstname',
      value: '',
      isOpen: false
    }
    this.toggle = this.toggle.bind(this)
  }

  toggleSplit = () => {
    this.setState({ splitButtonOpen: !this.state.splitButtonOpen })
  }

  changeButton = (current) => {
    this.setState({ current, url: current.toLowerCase().replace(/ /g, '') })
  }

  onChange = (ev) => {
    this.setState({value: ev.target.value})
  }

  checkEnterPress = (ev) => {
    if(ev.key === 'Enter') {
      this.search()
      this.setState({ value: '' })
    }
  }
  
  search = () => {
    history.push(`/search/${this.state.url}/${this.state.value}`)
  }

  toggle() {
    this.setState({ isOpen: !this.state.isOpen })
  }

  render() {
    const { accountID } = this.props
    return (
      <div className="Navbar">
        <Navbar color="primary" dark expand="md">
        <NavbarToggler onClick={this.toggle} />
          <NavbarBrand className="cliquer-brand"><img src={Logo} alt="" /></NavbarBrand>
          <Collapse isOpen={this.state.isOpen} navbar>
          <Nav className="mr-auto" navbar>
             <NavItem>
              <NavLink href="/groups">Groups</NavLink>
            </NavItem>
            <NavItem>
              <NavLink href="/create">Create a Group</NavLink>
            </NavItem> 
            <NavItem>
              <NavLink href="/public">Public Groups</NavLink>
            </NavItem> 
            <NavItem>
              <NavLink href={`/profile/${accountID ? accountID : ''}`}>Profile</NavLink>
            </NavItem>
            <NavItem>
              <NavLink href="/settings">Settings</NavLink>
            </NavItem> 
            {this.props.isMod() &&
            <NavItem>
              <NavLink href="/mod">Moderator Panel</NavLink>
            </NavItem>}
            </Nav>
            <Col sm={5}>
              <InputGroup>
                <InputGroupButtonDropdown addonType="prepend" isOpen={this.state.splitButtonOpen} toggle={this.toggleSplit}>
                  <Button>{this.state.current}</Button>
                  <DropdownToggle split />
                  <DropdownMenu>
                    <DropdownItem onClick={() => this.changeButton('First Name')}>First Name</DropdownItem>
                    <DropdownItem onClick={() => this.changeButton('Last Name')}>Last Name</DropdownItem>
                    <DropdownItem divider />
                    <DropdownItem onClick={() => this.changeButton('Skill')}>Skill</DropdownItem>
                    <DropdownItem onClick={() => this.changeButton('Reputation')}>Reputation</DropdownItem>
                    <DropdownItem divider />
                    <DropdownItem onClick={() => this.changeButton('Group')}>Group</DropdownItem>
                  </DropdownMenu>
                </InputGroupButtonDropdown>
                <Input placeholder="Search for friends" value={this.state.value} onKeyPress={this.checkEnterPress} onChange={this.onChange} />
                <InputGroupAddon addonType="append"><Button color="secondary" onClick={this.search}>Search</Button></InputGroupAddon>
              </InputGroup>
            </Col>
            <Nav className="ml-auto" navbar>
            <NavItem>
              <Button color="secondary" className="btn-sm" onClick={auth.logOut}>Log Out</Button>
            </NavItem>
            </Nav>
            </Collapse>
        </Navbar>
      </div>
    )
  }
}

export default NavigationBar

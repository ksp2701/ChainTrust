// SPDX-License-Identifier: MIT
pragma solidity ^0.8.17;

contract ChainTrust {
    address public owner;
    mapping(address => bytes32[]) public riskHistory;
    mapping(address => bool) public blacklist;

    event RiskStored(address indexed wallet, bytes32 indexed riskHash);
    event LoanRecorded(address indexed wallet, uint256 amount, bool approved);

    modifier onlyOwner() {
        require(msg.sender == owner, "owner only");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    function storeRiskHash(address wallet, bytes32 riskHash) external onlyOwner {
        riskHistory[wallet].push(riskHash);
        emit RiskStored(wallet, riskHash);
    }

    function recordLoan(address wallet, uint256 amount, bool approved) external onlyOwner {
        emit LoanRecorded(wallet, amount, approved);
    }

    function setBlacklist(address wallet, bool value) external onlyOwner {
        blacklist[wallet] = value;
    }

    function getRiskHistory(address wallet) external view returns (bytes32[] memory) {
        return riskHistory[wallet];
    }
}
